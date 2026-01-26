// 注册行为日志配置属性。
package com.shortvideo.recsys.backend.bigdata;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BehaviorLogProperties.class)
public class BehaviorLogConfig {
}
