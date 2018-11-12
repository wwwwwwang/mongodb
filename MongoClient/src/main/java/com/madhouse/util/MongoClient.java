package com.madhouse.util;

import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.lang3.StringUtils;
import org.bson.*;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by wujunfeng on 2018-03-30.
 */
public class MongoClient {
    private String databaseName = null;
    private MongoDatabase database = null;
    private com.mongodb.MongoClient mongoClient = null;

    public MongoClient(String brokers, String databaseName, String userName, String password, int maxConnTotal, int timeout) {
        MongoClientOptions.Builder options = MongoClientOptions.builder();

        options.connectTimeout(5000);
        //options.connectTimeout(10000);
        options.connectionsPerHost(maxConnTotal);
        options.socketTimeout(timeout);
        //options.threadsAllowedToBlockForConnectionMultiplier(1);
        options.maxWaitTime(10000);

        String[] hosts = brokers.split(",");
        List<ServerAddress> serverAddresses = new LinkedList<ServerAddress>();
        if (hosts != null && hosts.length > 0) {
            for (String host : hosts) {
                String[] str = host.split(":");
                if (str != null && str.length >= 2) {
                    ServerAddress serverAddress = new ServerAddress(str[0], Integer.parseInt(str[1]));
                    serverAddresses.add(serverAddress);
                }
            }
        }

        this.databaseName = databaseName;
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password)) {
            this.mongoClient = new com.mongodb.MongoClient(serverAddresses, options.build());
        } else {
            MongoCredential mongoCredential = MongoCredential.createCredential(userName, databaseName, password.toCharArray());
            this.mongoClient = new com.mongodb.MongoClient(serverAddresses, mongoCredential, options.build());
        }

        this.database = this.mongoClient.getDatabase(this.databaseName);
    }

    public MongoDatabase selectDatabase(String databaseName) {
        if (!StringUtils.isEmpty(databaseName)) {
            return (this.database = this.mongoClient.getDatabase(databaseName));
        }

        return null;
    }

    public MongoCollection<Document> getCollection(String collectionName) {
        if (this.database != null) {
            return this.database.getCollection(collectionName);
        }

        return null;
    }

    public Document queryById(String collectionName, String id) {
        try {
            ObjectId objectId = new ObjectId(id);
            return this.database.getCollection(collectionName).find(Filters.eq("_id", objectId)).first();
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return null;
    }

    public MongoCursor<Document> query(String collectionName, Bson filter) {
        try {
            return this.database.getCollection(collectionName).find(filter).iterator();
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return null;
    }

    public MongoCursor<Document> query(String collectionName, Bson filter, Bson orderBy) {
        try {
            return this.database.getCollection(collectionName).find(filter).sort(orderBy).iterator();
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return null;
    }

    public MongoCursor<Document> query(String collectionName, Bson filter, Bson orderBy, int pageNo, int pageSize) {
        try {
            return this.database.getCollection(collectionName).find(filter).sort(orderBy).skip(pageNo * pageSize).limit(pageSize).iterator();
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return null;
    }

    public Document queryById(MongoCollection<Document> mongoCollection, String id) {
        try {
            ObjectId objectId = new ObjectId(id);
            return mongoCollection.find(Filters.eq("_id", objectId)).first();
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return null;
    }

    public MongoCursor<Document> query(MongoCollection<Document> mongoCollection, Bson filter) {
        try {
            return mongoCollection.find(filter).iterator();
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return null;
    }

    public MongoCursor<Document> query(MongoCollection<Document> mongoCollection, Bson filter, Bson orderBy) {
        try {
            return mongoCollection.find(filter).sort(orderBy).iterator();
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return null;
    }

    public MongoCursor<Document> query(MongoCollection<Document> mongoCollection, Bson filter, Bson orderBy, int pageNo, int pageSize) {
        try {
            return mongoCollection.find(filter).sort(orderBy).skip(pageNo * pageSize).limit(pageSize).iterator();
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return null;
    }

    public boolean insert(String collectionName, List<Document> documents) {
        try {
            this.database.getCollection(collectionName).insertMany(documents);
            return true;
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return false;
    }

    public boolean update(String collectionName, Bson filter, Document document) {
        try {
            UpdateResult result = this.database.getCollection(collectionName).updateMany(filter, document);
            return true;
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return false;
    }

    public boolean upsert(String collectionName, Bson filter, Document document) {
        try {
            UpdateResult result = this.database.getCollection(collectionName).updateOne(filter, document);
            return true;
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return false;
    }

    /*
    updateAction:   1.field: currentDate inc min max mul rename set setOnInsert unset
                    2.array: addToSet pop pull push pullAll each position slice sort
     */
    public boolean upsert(String collectionName, String filterName, String filterValue, String updateAction, String updateName, String updateValue) {
        try {
            Bson filter = Filters.eq(filterName, filterValue);
            Bson update = new Document("$" + updateAction,
                    new Document().append(updateName, updateValue));
            UpdateOptions options = new UpdateOptions().upsert(true);
            UpdateResult result = this.database.getCollection(collectionName).updateOne(filter, update, options);
            return true;
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return false;
    }

    /*
    updateAction:   1.field: currentDate inc min max mul rename set setOnInsert unset
                    2.array: addToSet pop pull push pullAll each position slice sort
     */
    public boolean upsert(String collectionName, String filterName, String filterValue, String updateAction, String updateName, String[] updateValues) {
        try {
            Bson filter = Filters.eq(filterName, filterValue);
            BsonArray bsonArray = new BsonArray();
            for (String updateValue : updateValues) {
                bsonArray.add(new BsonString(updateValue));
            }
            Bson update = new Document("$" + updateAction,
                    new Document().append(updateName, new Document("$each", bsonArray)));
            UpdateOptions options = new UpdateOptions().upsert(true);
            UpdateResult result = this.database.getCollection(collectionName).updateOne(filter, update, options);
            return true;
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return false;
    }

    public boolean delete(String collectionName, Bson filter) {
        try {
            DeleteResult result = this.database.getCollection(collectionName).deleteMany(filter);
            return true;
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return false;
    }

    public void close() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
        }
    }
}
