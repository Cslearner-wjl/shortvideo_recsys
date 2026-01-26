// M3.2：Spark Structured Streaming 作业，消费 Kafka behavior-events 并写 Redis 实时统计与热门榜。
package com.shortvideo.recsys.streaming;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.expr;
import static org.apache.spark.sql.functions.from_json;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.sum;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;
import org.apache.spark.api.java.JavaRDD;

public class BehaviorStreamingJob {
    public static void main(String[] args) throws Exception {
        String kafkaBootstrap = env("KAFKA_BOOTSTRAP", "localhost:9093");
        String kafkaTopic = env("KAFKA_TOPIC", "behavior-events");
        String checkpointDir = env("CHECKPOINT_DIR", "/tmp/shortvideo_recsys/checkpoints/m3_2");
        String triggerInterval = env("SPARK_TRIGGER_INTERVAL", "5 seconds");

        RedisSinkConfig redisConfig = RedisSinkConfig.fromEnv();

        SparkSession spark = SparkSession.builder()
                .appName("shortvideo-recsys-m3_2-streaming")
                .getOrCreate();

        spark.conf().set("spark.sql.shuffle.partitions", envInt("SPARK_SHUFFLE_PARTITIONS", 8));

        Dataset<Row> kafka = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", kafkaBootstrap)
                .option("subscribe", kafkaTopic)
                .option("startingOffsets", env("KAFKA_STARTING_OFFSETS", "latest"))
                .load();

        Dataset<Row> events = kafka
                .selectExpr("CAST(value AS STRING) AS json")
                .select(from_json(col("json"), BehaviorEventSchemas.BEHAVIOR_EVENT_SCHEMA).as("e"))
                .select(
                        col("e.eventType").as("eventType"),
                        col("e.videoId").as("videoId")
                )
                .filter(col("eventType").isNotNull())
                .filter(col("videoId").isNotNull())
                .filter(col("videoId").gt(lit(0)));

        StreamingQuery q = events.writeStream()
                .trigger(Trigger.ProcessingTime(triggerInterval))
                .option("checkpointLocation", checkpointDir)
                .foreachBatch((batch, batchId) -> {
                    Dataset<Row> agg = batch
                            .groupBy(col("videoId"))
                            .agg(
                                    sum(expr("CASE WHEN eventType = 'PLAY' THEN 1 ELSE 0 END")).cast("long").alias("playDelta"),
                                    sum(expr("CASE WHEN eventType = 'LIKE' THEN 1 WHEN eventType = 'UNLIKE' THEN -1 ELSE 0 END")).cast("long").alias("likeDelta"),
                                    sum(expr("CASE WHEN eventType = 'COMMENT' THEN 1 ELSE 0 END")).cast("long").alias("commentDelta"),
                                    sum(expr("CASE WHEN eventType = 'FAVORITE' THEN 1 WHEN eventType = 'UNFAVORITE' THEN -1 ELSE 0 END")).cast("long").alias("favoriteDelta")
                            );

                    double wPlay = redisConfig.wPlay();
                    double wLike = redisConfig.wLike();
                    double wComment = redisConfig.wComment();
                    double wFavorite = redisConfig.wFavorite();

                    Dataset<Row> withHot = agg.withColumn(
                            "hotDelta",
                            col("playDelta").multiply(lit(wPlay))
                                    .plus(col("likeDelta").multiply(lit(wLike)))
                                    .plus(col("commentDelta").multiply(lit(wComment)))
                                    .plus(col("favoriteDelta").multiply(lit(wFavorite)))
                    );

                    JavaRDD<Row> rdd = withHot.toJavaRDD();
                    rdd.foreachPartition(rows -> RedisSink.writePartition(rows, redisConfig));
                })
                .start();

        q.awaitTermination();
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? defaultValue : v.trim();
    }

    private static int envInt(String key, int defaultValue) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
