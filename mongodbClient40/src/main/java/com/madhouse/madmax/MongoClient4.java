package com.madhouse.madmax;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonArray;
import org.bson.BsonValue;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

public class MongoClient4 {
    private String database;
    private String collection;
    private com.mongodb.client.MongoClient mongoClient;


    public MongoClient4(String hostport, String db, String table) {
        database = db;
        collection = table;
        mongoClient = MongoClients.create("mongodb://" + hostport);
    }

    public Map<String, List<String>> query(String id) {
        return query(id, "tags");
    }

    private Map<String, List<String>> query(String id, String fields) {
        MongoDatabase db = mongoClient.getDatabase(database);
        MongoCollection<Document> table = db.getCollection(collection);
        Map<String, List<String>> r = new HashMap<String, List<String>>();
        /*madhouse：列簇：cf，列名:mh_*
         talkingdata:列簇：cf,列名:td_*
		 unionpay-银联：列簇：cf,列名:up_*
		 admaster-admaster：列簇：cf,列名:am_*
		 qianxun-千寻：列簇：cf,列名:qx_*
		  科大-：列簇：cf,列名:kd_*
		 数据表：maddsp_threedata_merge*/
        List<String> up = new ArrayList<String>();
        List<String> md = new ArrayList<String>();
        List<String> td = new ArrayList<String>();
        List<String> am = new ArrayList<String>();
        List<String> qx = new ArrayList<String>();
        List<String> kd = new ArrayList<String>();
        List<String> sz = new ArrayList<String>();
        List<String> ap = new ArrayList<String>();

        Document projection = new Document("_id", 0);
        for (String f : fields.split(",")) {
            projection.append(f, 1);
        }

        Document res = table.find(eq("_id", id)).projection(projection).first();
        if (res != null && res.size() > 0) {
            //System.out.println(res.toJson());
            if (res.containsKey("tags")) {
                ArrayList tags = res.get("tags", ArrayList.class);
                for (Object tag : tags) {
                    String t = tag.toString();
                    String prefix = t.split("_")[0];
                    String label = t.split("_")[1];
                    try {
                        if ("td".equals(prefix)) {
                            td.add(label);
                        } else if ("mh".equals(prefix)) {
                            md.add(label);
                        } else if ("up".equals(prefix)) {
                            up.add(label);
                        } else if ("am".equals(prefix)) {
                            am.add(label);
                        } else if ("qx".equals(prefix)) {
                            qx.add(label);
                        } else if ("kd".equals(prefix)) {
                            kd.add(label);
                        } else if ("sz".equals(prefix)) {
                            sz.add(label);
                        } else if ("ap".equals(prefix)) {
                            ap.add(label);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        r.put("md", md);
        r.put("td", td);
        r.put("up", up);
        r.put("am", am);
        r.put("qx", qx);
        r.put("kd", kd);
        r.put("sz", sz);
        r.put("ap", ap);
        return r;
    }
}
