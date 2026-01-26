// 行为日志配置，控制日志路径与开关。
package com.shortvideo.recsys.backend.bigdata;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.behavior-log")
public class BehaviorLogProperties {
    private boolean enabled = true;
    private String path = "./logs/behavior-events.log";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
