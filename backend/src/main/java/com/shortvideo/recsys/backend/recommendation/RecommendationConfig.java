package com.shortvideo.recsys.backend.recommendation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"docker", "test"})
@EnableConfigurationProperties(RecommendationProperties.class)
public class RecommendationConfig {
}

