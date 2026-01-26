// M4：JDBC 读取 user_actions 并为 ALS 训练提供数据源。
package com.shortvideo.recsys.batch;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class JdbcUserActionsReader {
    public static Dataset<Row> read(SparkSession spark, String jdbcUrl, String user, String password, String tableOrQuery) {
        return spark.read()
                .format("jdbc")
                .option("url", jdbcUrl)
                .option("dbtable", tableOrQuery)
                .option("user", user)
                .option("password", password)
                .option("driver", "com.mysql.cj.jdbc.Driver")
                .load();
    }
}

