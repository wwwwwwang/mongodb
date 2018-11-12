
name := "mongodbSpark"

version := "1.0"

scalaVersion := "2.10.6"

libraryDependencies += "com.typesafe" % "config" % "1.3.0"
libraryDependencies += "org.apache.hadoop" % "hadoop-client" % "2.0.0-mr1-cdh4.7.0" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "1.6.2" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-hive" % "1.6.2" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-core" % "1.6.2" % "provided"
libraryDependencies += "commons-cli" % "commons-cli" % "1.2"
libraryDependencies += "org.mongodb.spark" % "mongo-spark-connector_2.10" % "1.1.0"

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".class" => MergeStrategy.first
  case "application.conf" => MergeStrategy.concat
  case "unwanted.txt" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
    