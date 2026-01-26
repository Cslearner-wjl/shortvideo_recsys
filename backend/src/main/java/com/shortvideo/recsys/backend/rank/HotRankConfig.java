package com.shortvideo.recsys.backend.rank;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"docker", "test"})
@EnableConfigurationProperties(HotRankProperties.class)
public class HotRankConfig {
}
