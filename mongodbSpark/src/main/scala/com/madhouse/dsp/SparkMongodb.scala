package com.madhouse.dsp

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.{Filters, UpdateOptions}
import com.mongodb.spark._
import com.mongodb.spark.config.WriteConfig
import org.apache.commons.cli._
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import org.bson.conversions.Bson
import org.bson.{BsonArray, BsonString, Document}

/**
  * Created by Madhouse on 2018/4/25.
  */
object SparkMongodb {

  def addPostfix(s: String): String = {
    if (s.contains("-") && s.length == 36)
      s"$s:ifa"
    else if (!s.contains("-") && s.length == 32)
      s"$s:didmd5"
    else
      s
  }

  def main(args: Array[String]): Unit = {

    var broker = "127.0.0.1:27017"
    var collection = ""
    var database = ""
    var file = ""
    var json = false
    var update = false
    var remove = false
    var tags = ""

    val opt = new Options()
    opt.addOption("b", "broker", true, "brokers of mongodb")
    opt.addOption("c", "collection", true, "collection name in mongodb")
    opt.addOption("d", "database", true, "database name in mongodb")
    opt.addOption("f", "file", true, "read file path in hdfs")
    opt.addOption("j", "json", false, "the file is json file")
    opt.addOption("h", "help", false, "help message")
    opt.addOption("u", "update", false, "whether add a tag to tags set")
    opt.addOption("r", "remove", false, "whether remove a tag from tags set")
    opt.addOption("t", "tags", true, "specify tag(s) be removed or added")


    val formatstr = "sh run.sh yarn-cluster|yarn-client|local ...."
    val formatter = new HelpFormatter
    val parser = new PosixParser

    var cl: CommandLine = null
    try
      cl = parser.parse(opt, args)

    catch {
      case e: ParseException =>
        e.printStackTrace()
        formatter.printHelp(formatstr, opt)
        System.exit(1)
    }
    if (cl.hasOption("b")) broker = cl.getOptionValue("b")
    if (cl.hasOption("c")) collection = cl.getOptionValue("c")
    if (cl.hasOption("d")) database = cl.getOptionValue("d")
    if (cl.hasOption("f")) file = cl.getOptionValue("f")
    if (cl.hasOption("h")) {
      formatter.printHelp(formatstr, opt)
      System.exit(0)
    }
    if (cl.hasOption("j")) json = true
    if (cl.hasOption("u")) update = true
    if (cl.hasOption("r")) remove = true
    if (cl.hasOption("t")) tags = cl.getOptionValue("t")

    val sparkConf = new SparkConf().setAppName("sparkMongodb")
    sparkConf.set("spark.mongodb.input.uri", s"mongodb://$broker/$database.$collection?readPreference=primaryPreferred")
    sparkConf.set("spark.mongodb.output.uri", s"mongodb://$broker/$database.$collection")
    val sc = new SparkContext(sparkConf)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    println(s"#####mongo db broker = $broker , choose database = $database, collection = $collection, " +
      s"file path = $file, file is json ? : $json, update = $update, remove = $remove, tags = $tags")
    //conf "spark.mongodb.input.uri=mongodb://127.0.0.1/test.myCollection?readPreference=primaryPreferred" \
    //conf "spark.mongodb.output.uri=mongodb://127.0.0.1/test.myCollection"
    val start = System.currentTimeMillis()
    val df =
      if (remove || update) sqlContext.read.text(file)
      else if (json) sqlContext.read.json(file)
      else sqlContext.read.parquet(file)
    val mdf =
      if (remove || update)
        df.select('value as "_id").cache
      else
      //did, os, tags
        df.select('did as "_id", org.apache.spark.sql.functions.split('tags, ",") as "tags", 'os as "os").cache
    mdf.show(20, truncate = false)
    println(s"##### df has ${mdf.count()} records...")

    val documents = mdf.toJSON.map(Document.parse)

    println(s"##### start to save...")
    if (update) {
      //val action = if(!remove) "$addToSet" else "$pullAll"
      val conf = WriteConfig(sc)
      val mongoConnector = MongoConnector(conf.asOptions)
      documents.foreachPartition(iter =>
        if (iter.nonEmpty) mongoConnector.withCollectionDo(conf, { collection: MongoCollection[Document] =>
          //iter.grouped(512).foreach(batch => collection.insertMany(batch.toList.asJava))
          val options: UpdateOptions = new UpdateOptions().upsert(true)
          iter.foreach(e => {
            val filter: Bson = Filters.eq("_id", addPostfix(e.getString("_id")))
            val bsonArray: BsonArray = new BsonArray
            //for (updateValue <- e.getString("tags").split(",")) {
            for (tag <- tags.split(",")) {
              bsonArray.add(new BsonString(tag))
            }
            val update: Bson =
              new Document("$addToSet", new Document().append("tags", new Document("$each", bsonArray)))
            collection.updateOne(filter, update, options)
          })
        }))
    } else if (remove) {
      val conf = WriteConfig(sc)
      val mongoConnector = MongoConnector(conf.asOptions)
      documents.foreachPartition(iter =>
        if (iter.nonEmpty) mongoConnector.withCollectionDo(conf, { collection: MongoCollection[Document] =>
          //iter.grouped(512).foreach(batch => collection.insertMany(batch.toList.asJava))
          val options: UpdateOptions = new UpdateOptions().upsert(true)
          val bsonArray: BsonArray = new BsonArray
          for (tag <- tags.split(",")) {
            bsonArray.add(new BsonString(tag))
          }
          iter.foreach(e => {
            val filter: Bson = Filters.eq("_id", addPostfix(e.getString("_id")))
            val update: Bson =
              new Document("$pullAll", new Document().append("tags", bsonArray))
            collection.updateOne(filter, update, options)
          })
        }))
    } else {
      MongoSpark.save(documents)
    }
    mdf.unpersist()
    println(s"#####===finished, use time: ${(System.currentTimeMillis() - start) / 1000}s!")
    sc.stop()
  }
}
