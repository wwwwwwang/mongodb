package com.madhouse.madmax;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class clientTest {
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
            String line;
            int cnt = 0;
            while ((line = br.readLine()) != null) {
                if (line.length() == 32 || line.length() == 36) {
                    if(line.contains("-"))
                        res.add(line.toUpperCase()+":ifa");
                    else
                        res.add(line.toLowerCase()+":didmd5");
                    cnt++;
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
            } catch (Exception ignored) {
            }
        }
        return res;
    }

    public static void main(String[] args) {
        Options opt = new Options();
        opt.addOption("b", "broker", true, "the brokers list of mongodb server, such as 127.0.0.1:20000");
        opt.addOption("c", "collections", true, "the collection name will be used");
        opt.addOption("d", "database", true, "the database name will be used");
        opt.addOption("h", "help", false, "help message");
        opt.addOption("p", "path", true, "the file name used to make the path of input file");
        opt.addOption("t", "thread", true, "the count of threads, default: 10");
        opt.addOption("s", "show", false, "whether show query result sample in the end");

        String basePath = "/home/wanghaishen/apps/";
        String path = "MongoClient4/id2.csv";
        int threadCount = 10;
        String broker = "10.10.16.62:27028";
        String database = "madmax";
        String collection = "user_tags";
        Boolean show = false;

        String formatstr = "java -jar *-SNAPSHOT-jar-with-dependencies.jar ...";
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
            broker = cl.getOptionValue("b");
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
        if (cl.hasOption("p")) {
            path = cl.getOptionValue("p");
        }
        if (cl.hasOption("s")) {
            show  = true;
        }
        if (cl.hasOption("t")) {
            threadCount = Integer.parseInt(cl.getOptionValue("t"));
        }

        String filePath = basePath + path;

        System.out.println("##### broker=" + broker + ", database = " + database + ", table = " + collection +
                ", path = " + filePath + ", threadcount = " + threadCount + ", show = " + show);

        final MongoClient4 client = new MongoClient4(broker, database, collection);

        System.out.println("###start..");
        Long s = System.currentTimeMillis();
        final ArrayList<String> ids = getByteArrayFromFile(filePath);
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < ids.size(); i++) {
            final int finalI = i;
            Runnable run = new Runnable() {
                public void run() {
                    try {
                        String did = ids.get(finalI);
                        if (did.contains("-")) {
                            did = did.toUpperCase();
                        }
                        long st = System.currentTimeMillis();
                        Map<String, List<String>> results = client.query(did);
                        if (results != null) {
                            /*for (Row row : results) {
                                System.out.println("#####tags size = " + row.getSet("tag", String.class).size());
                            }*/
                            System.out.println("#####get records by did list finishes with time costing " + (System.currentTimeMillis() - st) + "ms");
                        } else {
                            System.out.println("#####failed: " + did);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            exec.execute(run);
        }
        exec.shutdown();
        try {
            while (!exec.isTerminated()) {
                Thread.sleep(200);
                //System.out.println("Executors is not terminated!");
            }
            System.out.println("#####end...");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Long e = System.currentTimeMillis();
        System.out.println("##### time cost = " + (e - s) / 1000 + "s!");

        if(show){
            for (int i = 0; i < 20; i++) {
                String did = ids.get(i);
                System.out.println("#######for did: "+ did);
                Map<String, List<String>> results = client.query(did);
                for(String key : results.keySet()){
                    StringBuilder labels = new StringBuilder();
                    for (String l : results.get(key)){
                        labels.append(l).append(",");
                    }
                    System.out.println("  "+key+":" + labels);
                }
                System.out.println("#######################################################");
            }
        }

        System.exit(0);
    }
}
