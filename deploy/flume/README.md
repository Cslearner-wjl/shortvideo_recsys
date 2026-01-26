# Flume 行为日志采集

## 目录结构
- `flume.conf`：TAILDIR 采集 `/data/behavior/behavior-events.log` 并写入 Kafka `behavior-events`

## Docker 方式
1. 准备日志目录（示例：`deploy/data/behavior/`）
2. 启动依赖：
```
cd deploy
docker-compose up -d zookeeper kafka flume
```
3. 后端设置日志路径（指向容器挂载目录）：
```
export BEHAVIOR_LOG_PATH=/data/behavior/behavior-events.log
```

> 说明：docker compose 默认使用 `probablyfine/flume` 镜像，配置文件挂载到 `/opt/flume-config/flume.conf`，并通过 `FLUME_AGENT_NAME=agent` 启动。

## 本地方式
```
flume-ng agent -n agent -c conf -f deploy/flume/flume.conf -Dflume.root.logger=INFO,console
```

## 注意事项
- `behavior-events.log` 为 JSON Lines，每行一个事件
- Flume 依赖 Kafka 可用
