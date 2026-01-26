// M4：Spark MLlib ALS（implicit feedback）离线训练，输出每个用户 TopN 推荐写入 Redis。
package com.shortvideo.recsys.batch;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.expr;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.max;
import static org.apache.spark.sql.functions.row_number;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;
import org.apache.spark.sql.functions;

public class AlsTrainJob {
    public static void main(String[] args) {
        String sparkAppName = env("SPARK_APP_NAME", "shortvideo-recsys-m4-als-batch");
        SparkSession spark = SparkSession.builder().appName(sparkAppName).getOrCreate();
        spark.conf().set("spark.sql.shuffle.partitions", envInt("SPARK_SHUFFLE_PARTITIONS", 8));

        String source = env("ALS_SOURCE", "jdbc"); // jdbc | csv
        String inputType = env("ALS_INPUT_TYPE", "user_actions"); // user_actions | ratings

        Dataset<Row> actions = null;
        Dataset<Row> ratings;

        if ("ratings".equalsIgnoreCase(inputType)) {
            ratings = loadRatings(spark, source);
        } else {
            actions = loadUserActions(spark, source);
            ratings = buildRatingsFromActions(actions);
        }

        ratings = ratings
                .filter(col("user_id").isNotNull())
                .filter(col("video_id").isNotNull())
                .filter(col("rating").isNotNull())
                .filter(col("user_id").gt(lit(0)))
                .filter(col("video_id").gt(lit(0)))
                .filter(col("rating").gt(lit(0)));

        WindowSpec userWin = Window.orderBy(col("user_id"));
        WindowSpec itemWin = Window.orderBy(col("video_id"));

        Dataset<Row> userMap = ratings.select(col("user_id"))
                .distinct()
                .orderBy(col("user_id"))
                .withColumn("user", row_number().over(userWin).minus(lit(1)).cast("int"));

        Dataset<Row> itemMap = ratings.select(col("video_id"))
                .distinct()
                .orderBy(col("video_id"))
                .withColumn("item", row_number().over(itemWin).minus(lit(1)).cast("int"));

        Dataset<Row> train = ratings
                .join(userMap, "user_id")
                .join(itemMap, "video_id")
                .select(col("user"), col("item"), col("rating").cast("float").alias("rating"));

        int rank = envInt("ALS_RANK", 20);
        int maxIter = envInt("ALS_MAX_ITER", 10);
        double regParam = envDouble("ALS_REG_PARAM", 0.1);
        double alpha = envDouble("ALS_ALPHA", 40.0);
        int topN = envInt("ALS_TOPN", 50);

        ALS als = new ALS()
                .setUserCol("user")
                .setItemCol("item")
                .setRatingCol("rating")
                .setImplicitPrefs(true)
                .setRank(rank)
                .setMaxIter(maxIter)
                .setRegParam(regParam)
                .setAlpha(alpha)
                .setColdStartStrategy("drop");

        ALSModel model = als.fit(train);

        Dataset<Row> rawRecs = model.recommendForAllUsers(topN);

        Dataset<Row> exploded = rawRecs
                .select(col("user"), functions.posexplode(col("recommendations")).as(new String[]{"pos", "rec"}))
                .select(
                        col("user"),
                        col("pos"),
                        col("rec.item").cast("int").alias("item")
                );

        Dataset<Row> joined = exploded
                .join(userMap, "user")
                .join(itemMap, "item")
                .select(col("user_id"), col("pos"), col("video_id").alias("videoId"));

        Dataset<Row> perUser = joined.groupBy(col("user_id"))
                .agg(expr("transform(sort_array(collect_list(named_struct('pos', pos, 'videoId', videoId))), x -> x.videoId) as video_ids"));

        RedisRecommendationWriter.Config redisCfg = RedisRecommendationWriter.Config.fromEnv();
        JavaRDD<Row> rdd = perUser.toJavaRDD();
        rdd.foreachPartition(rows -> RedisRecommendationWriter.writePartition(rows, redisCfg));

        if (actions != null && "1".equals(env("EVAL_HIT_ENABLED", "0"))) {
            evalHitAtK(spark, actions, perUser, envInt("EVAL_HIT_K", 20));
        }

        spark.stop();
    }

    private static Dataset<Row> loadRatings(SparkSession spark, String source) {
        String defaultPath = "bigdata/batch/sample-data/ratings.csv";
        String csvPath = env("ALS_CSV_PATH", defaultPath);
        if (!"csv".equalsIgnoreCase(source)) {
            source = "csv";
        }
        return spark.read()
                .option("header", "true")
                .option("inferSchema", "true")
                .csv(csvPath)
                .select(
                        col("user_id").cast("long"),
                        col("video_id").cast("long"),
                        col("rating").cast("double")
                );
    }

    private static Dataset<Row> loadUserActions(SparkSession spark, String source) {
        int days = envInt("ALS_TRAIN_DAYS", 7);
        if ("csv".equalsIgnoreCase(source)) {
            String defaultPath = "bigdata/batch/sample-data/user_actions.csv";
            String csvPath = env("ALS_CSV_PATH", defaultPath);
            return spark.read()
                    .option("header", "true")
                    .option("inferSchema", "true")
                    .csv(csvPath)
                    .select(
                            col("user_id").cast("long"),
                            col("video_id").cast("long"),
                            col("action_type").cast("string"),
                            col("action_time").cast("timestamp")
                    );
        }

        String jdbcUrl = env("JDBC_URL", "");
        String jdbcUser = env("JDBC_USER", "app");
        String jdbcPassword = env("JDBC_PASSWORD", "apppass");
        String jdbcTable = env("JDBC_TABLE", "user_actions");

        if (jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("缺少 JDBC_URL（ALS_SOURCE=jdbc 时必填）");
        }

        String query = "("
                + "SELECT user_id, video_id, action_type, action_time "
                + "FROM " + jdbcTable + " "
                + "WHERE action_type IN ('PLAY','LIKE','COMMENT','FAVORITE') "
                + "AND action_time >= (NOW() - INTERVAL " + Math.max(0, days) + " DAY)"
                + ") t";

        return JdbcUserActionsReader.read(spark, jdbcUrl, jdbcUser, jdbcPassword, query)
                .select(
                        col("user_id").cast("long"),
                        col("video_id").cast("long"),
                        col("action_type").cast("string"),
                        col("action_time").cast("timestamp")
                );
    }

    private static Dataset<Row> buildRatingsFromActions(Dataset<Row> actions) {
        Dataset<Row> withW = actions
                .withColumn("w", expr(
                        "CASE "
                                + "WHEN action_type = 'FAVORITE' THEN 4 "
                                + "WHEN action_type = 'COMMENT' THEN 3 "
                                + "WHEN action_type = 'LIKE' THEN 2 "
                                + "WHEN action_type = 'PLAY' THEN 1 "
                                + "ELSE 0 "
                                + "END"
                ));

        return withW.groupBy(col("user_id"), col("video_id"))
                .agg(functions.sum(col("w")).cast("double").alias("rating"));
    }

    private static void evalHitAtK(SparkSession spark, Dataset<Row> actions, Dataset<Row> perUserRecs, int k) {
        int safeK = Math.max(1, Math.min(k, 200));

        Dataset<Row> latest = actions.groupBy(col("user_id")).agg(max(col("action_time")).alias("max_time"));
        Dataset<Row> holdout = actions
                .join(latest, actions.col("user_id").equalTo(latest.col("user_id"))
                        .and(actions.col("action_time").equalTo(latest.col("max_time"))))
                .select(actions.col("user_id"), actions.col("video_id"))
                .dropDuplicates("user_id");

        Dataset<Row> topK = perUserRecs
                .select(col("user_id"), expr("slice(video_ids, 1, " + safeK + ") as topk"));

        Dataset<Row> joined = holdout.join(topK, "user_id");
        long total = joined.count();
        if (total <= 0) {
            System.out.println("[EVAL] hit@" + safeK + ": N/A（没有可评估样本）");
            return;
        }

        long hits = joined.filter(expr("array_contains(topk, video_id)")).count();
        double hitRate = hits * 1.0 / total;
        System.out.println("[EVAL] hit@" + safeK + ": " + hits + "/" + total + " = " + hitRate);
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

    private static double envDouble(String key, double defaultValue) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
