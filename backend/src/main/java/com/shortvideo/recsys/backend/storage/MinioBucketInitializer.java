package com.shortvideo.recsys.backend.storage;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("docker")
public class MinioBucketInitializer {
    @Bean
    public ApplicationRunner minioBucketInitRunner(MinioStorageService storageService) {
        return args -> {
            int attempts = 0;
            while (true) {
                try {
                    storageService.ensureBucket();
                    return;
                } catch (Exception e) {
                    attempts++;
                    if (attempts >= 30) {
                        throw e;
                    }
                    Thread.sleep(1000L);
                }
            }
        };
    }
}
