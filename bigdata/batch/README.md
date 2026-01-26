# M4 离线推荐（Spark MLlib ALS -> Redis）

本目录提供 Spark 离线批处理作业：从 `user_actions` 构建隐式反馈训练数据，训练 ALS 模型并将每个用户的 TopN 推荐写入 Redis：

- `rec:user:{userId}`（List，元素为 `videoId` 字符串）

约束：
- 不引入 HDFS（支持 JDBC 或本地 CSV）
- 不做模型自动调参
- 不做复杂评估指标（可选输出简单 hit@K）

---

## 1. 目录结构

```text
bigdata/batch/
  pom.xml
  README.md
  bin/
    ensure_spark.sh
    run_als.sh
  sample-data/
    user_actions.csv
    ratings.csv
  src/main/java/com/shortvideo/recsys/batch/
    AlsTrainJob.java
    JdbcUserActionsReader.java
    RedisRecommendationWriter.java
```

---

## 2. 构建

`mvn -f bigdata/batch/pom.xml -DskipTests package`

产物：`bigdata/batch/target/batch-0.1.0.jar`（包含 jedis + mysql driver；Spark 核心类由 Spark Distribution 提供）。

---

## 3. 运行

### 3.1 推荐：使用项目内固定 Spark 3.5.1

```
bigdata/batch/bin/run_als.sh --help
bigdata/batch/bin/run_als.sh
```

脚本会复用 `bigdata/streaming/bin/ensure_spark.sh` 下载/缓存 Spark 3.5.1（默认缓存目录 `bigdata/streaming/.spark/`）。

离线/自定义 Spark 发行版时，可设置：

```
export SPARK_DIST=/path/to/spark-3.5.1-bin-hadoop3
bigdata/batch/bin/run_als.sh
```

---

## 4. 输入与评分映射

支持两类输入（通过 `ALS_INPUT_TYPE` 控制）：

1) `ALS_INPUT_TYPE=user_actions`（默认）
- JDBC：读取 MySQL 表 `user_actions`
- CSV：读取 `sample-data/user_actions.csv` 同名列
- 评分映射（implicit feedback 强度）：
  - `PLAY` -> 1
  - `LIKE` -> 2
  - `COMMENT` -> 3
  - `FAVORITE` -> 4

2) `ALS_INPUT_TYPE=ratings`
- CSV：读取 `sample-data/ratings.csv`（列：`user_id,video_id,rating`）

聚合策略（推荐）：按 `(user_id, video_id)` 聚合求和。

---

## 5. 关键环境变量

### Spark
- `SPARK_MASTER`：默认 `local[*]`
- `SPARK_SHUFFLE_PARTITIONS`：默认 `8`

### 输入来源
- `ALS_SOURCE`：`jdbc`（默认）或 `csv`
- `ALS_INPUT_TYPE`：`user_actions`（默认）或 `ratings`
- `ALS_CSV_PATH`：CSV 路径（默认按 input_type 选择 sample-data）
- `ALS_TRAIN_DAYS`：训练窗口天数（仅 `user_actions` 适用，默认 `7`）

### JDBC（ALS_SOURCE=jdbc）
- `JDBC_URL`：例如 `jdbc:mysql://127.0.0.1:3307/shortvideo_recsys?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai`
- `JDBC_USER`：默认 `app`
- `JDBC_PASSWORD`：默认 `apppass`
- `JDBC_TABLE`：默认 `user_actions`

### ALS 参数
- `ALS_RANK`：默认 `20`
- `ALS_MAX_ITER`：默认 `10`
- `ALS_REG_PARAM`：默认 `0.1`
- `ALS_ALPHA`：默认 `40.0`
- `ALS_TOPN`：默认 `50`

### Redis 输出
- `REDIS_HOST`：默认 `127.0.0.1`
- `REDIS_PORT`：默认 `6379`
- `REDIS_PASSWORD`：可选
- `REDIS_DB`：默认 `0`
- `REDIS_KEY_PREFIX`：默认 `rec:user:`
- `REDIS_TTL_SECONDS`：默认 `86400`（24h）

### 可选评估（简单 hit@K）
- `EVAL_HIT_ENABLED`：`1` 开启（仅 `ALS_INPUT_TYPE=user_actions` 且存在 `action_time` 时有效）
- `EVAL_HIT_K`：默认 `20`

---

## 6. 最小可复现（CSV）

1) 启动 Redis（docker 例子）：

`docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d redis`

2) 运行（CSV ratings）：

```
export ALS_SOURCE=csv
export ALS_INPUT_TYPE=ratings
export ALS_CSV_PATH=bigdata/batch/sample-data/ratings.csv
bigdata/batch/bin/run_als.sh
```

3) 验证 Redis：

```
redis-cli -h 127.0.0.1 -p 6379 LRANGE rec:user:1 0 9
redis-cli -h 127.0.0.1 -p 6379 LRANGE rec:user:2 0 9
```

