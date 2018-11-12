package com.madhouse.util;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.bson.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wujunfeng on 2018-03-30.
 */
public class Main {
    private static ArrayList<String> getByteArrayFromFile(String path) {
        File readFile = new File(path);
        InputStream in = null;
        InputStreamReader ir = null;
        BufferedReader br = null;
        ArrayList<String> res = new ArrayList<String>();

        try {
            in = new BufferedInputStream(new FileInputStream(readFile));
            ir = new InputStreamReader(in, "utf-8");
            br = new BufferedReader(ir);
            String line = "";
            int cnt = 0;
            while ((line = br.readLine()) != null) {
                if (line.length() > 3) {
                    String rowkey = line.toLowerCase();
                    cnt++;
                    res.add(rowkey);
                }
            }
            System.out.println("there are " + cnt + " records in the file:" + path);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (ir != null) {
                    ir.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
            }
        }
        return res;
    }

    private static String[] string2Array(String str) {
        String subStr = str.substring(str.indexOf("[") + 1, str.indexOf("]")).replaceAll("\"", "");
        return subStr.split(",");
    }

    private static String getDid(String str) {
        return str.substring(str.indexOf(":") + 1, str.indexOf(",")).replaceAll("\"", "");
    }

    public static void main(String[] args) {
        Options opt = new Options();
        opt.addOption("b", "broker", true, "the brokers list of mongodb server, such as 127.0.0.1:20000");
        opt.addOption("c", "collections", true, "the collection name will be used");
        opt.addOption("d", "database", true, "the database name will be used");
        opt.addOption("h", "help", false, "help message");
        opt.addOption("k", "key", true, "the filed name(key) of one collection");
        opt.addOption("mc", "max-connection", true, "the max count of connection");
        opt.addOption("p", "path", true, "the file name used to make the path of input file");
        opt.addOption("pw", "password", true, "the password used to connect to mongodb");
        opt.addOption("t", "thread", true, "the count of threads, default: 10");
        opt.addOption("to", "timeout", true, "the time out of socket");
        opt.addOption("u", "username", true, "the user name used to connect to mongodb");
        opt.addOption("us", "upsert", false, "choose function upsert");

        int threadCount = 10;
        String path = "20";
        String brokers = "127.0.0.1:20000";
        String userName = "";
        String password = "";
        String collection = "w";
        String database = "whsh";
        String key = "did";
        int timeOut = 30;
        int maxConn = 100;
        Boolean find = true;
        Boolean upsert = false;

        String formatstr = "java -jar mongoclient-1.0-SNAPSHOT-jar-with-dependencies.jar ...";
        HelpFormatter formatter = new HelpFormatter();
        PosixParser parser = new PosixParser();

        CommandLine cl = null;
        try {
            cl = parser.parse(opt, args);
        } catch (Exception e) {
            e.printStackTrace();
            formatter.printHelp(formatstr, opt);
            System.exit(1);
        }

        if (cl.hasOption("b")) {
            brokers = cl.getOptionValue("b");
        }
        if (cl.hasOption("c")) {
            collection = cl.getOptionValue("c");
        }
        if (cl.hasOption("d")) {
            database = cl.getOptionValue("d");
        }
        if (cl.hasOption("h")) {
            formatter.printHelp(formatstr, opt);
            System.exit(0);
        }
        if (cl.hasOption("k")) {
            key = cl.getOptionValue("k");
        }
        if (cl.hasOption("mc")) {
            maxConn = Integer.parseInt(cl.getOptionValue("mc"));
        }
        if (cl.hasOption("p")) {
            path = cl.getOptionValue("p");
        }
        if (cl.hasOption("pw")) {
            password = cl.getOptionValue("pw");
        }
        if (cl.hasOption("t")) {
            threadCount = Integer.parseInt(cl.getOptionValue("t"));
        }
        if (cl.hasOption("to")) {
            timeOut = Integer.parseInt(cl.getOptionValue("to"));
        }
        if (cl.hasOption("u")) {
            userName = cl.getOptionValue("u");
        }
        if (cl.hasOption("us")) {
            find = false;
            upsert = true;
        }

        System.out.println("##### brokers=" + brokers +
                ", collection = " + collection +
                ", database = " + database +
                ", max connection = " + maxConn +
                ", path = " + path +
                ", threadcount = " + threadCount +
                ", time out = " + timeOut +
                ", user name = " + userName +
                ", password = " + password +
                ", key " + key +
                ", find = " + find +
                ", upsert = " + upsert);
        //MongoClient client = new MongoClient("127.0.0.1:27017", "database", "user", "password", 100, 30);
        final MongoClient client = new MongoClient(brokers, database, userName, password, maxConn, timeOut);

        System.out.println("###start..");

        if (find) {
            String filePath = "/home/wanghaishen/apps/mongoclient/id" + path + ".csv";
            final ArrayList<String> ids = getByteArrayFromFile(filePath);
            final String c = collection;
            ExecutorService exec = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < ids.size(); i++) {
                final int finalI = i;
                final String finalKey = key;
                Runnable run = new Runnable() {
                    public void run() {
                        try {
                            long st = System.currentTimeMillis();
                            String did = ids.get(finalI);
                            if (did.contains("-")){
                                did = did.toUpperCase();
                            }
                            MongoCursor<Document> documents = client.query(c, Filters.eq(finalKey, did));
                            if (documents != null && documents.hasNext()) {
                                Document doc = documents.next();
                                System.out.println(doc.toJson());
                                System.out.println("#####get records by did list finishes with time costing " + (System.currentTimeMillis() - st) + "ms");
                            } else {
                                System.out.println("#####failed: " + did);
                            }
                            Thread.sleep(500);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                exec.execute(run);
            }

        /*MongoCursor<Document> documents = client.query("tablename", Filters.eq("did", "8cc422c2dca40ca3ca57fff0243479e1:didmd5"));
        while (documents != null && documents.hasNext()) {
            Document doc = documents.next();
            System.out.println(doc.toJson());
        }*/
            /*int a = (Integer.parseInt(path) / threadCount + 10) * 1000;
            try {
                Thread.sleep(a);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            //client.close();
        } else if (upsert) {
            String filePath = "/home/wanghaishen/apps/mongoclient/" + path + ".json";
            ArrayList<String> ids = getByteArrayFromFile(filePath);
            for (String str : ids) {
                String did = getDid(str);
                String[] tags = string2Array(str);
                Boolean res =  client.upsert(collection, key, did, "addToSet", "tags", tags);
                System.out.println("##### did: " + did + " has been dealed, res is "+(res?"succeed":"failed"));
            }
        }
        System.out.println("#####end...");
    }
}
