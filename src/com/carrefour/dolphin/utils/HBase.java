package com.carrefour.dolphin.utils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import java.io.IOException;

public class HBase {

    public static Configuration getHaddopConfig() {
        Configuration conf = HBaseConfiguration.create();
        conf.addResource("hbase-site.xml");
        conf.addResource("core-site.xml");
        conf.addResource("hdfs-site.xml");
        return conf;
    }

    public static Connection getHBaseConnection() throws IOException {
        Configuration config = getHaddopConfig();
        return ConnectionFactory.createConnection(config);
    }

}
