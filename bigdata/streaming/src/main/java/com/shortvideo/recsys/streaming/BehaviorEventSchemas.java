// 行为事件 Schema：用于 Spark from_json 解析 Kafka value(JSON)。
package com.shortvideo.recsys.streaming;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

public final class BehaviorEventSchemas {
    private BehaviorEventSchemas() {
    }

    public static final StructType BEHAVIOR_EVENT_SCHEMA = new StructType()
            .add("eventId", DataTypes.StringType, true)
            .add("eventType", DataTypes.StringType, true)
            .add("userId", DataTypes.LongType, true)
            .add("videoId", DataTypes.LongType, true)
            .add("actionTime", DataTypes.StringType, true)
            .add("actionTs", DataTypes.LongType, true)
            .add("durationMs", DataTypes.LongType, true)
            .add("isCompleted", DataTypes.BooleanType, true)
            .add("source", DataTypes.StringType, true);
}

