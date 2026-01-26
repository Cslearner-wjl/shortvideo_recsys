#!/bin/bash

# 追加 Spark 自带 Hadoop 客户端依赖，供 Flume HDFS Sink 使用
export FLUME_CLASSPATH="/opt/flume-classpath/*"
