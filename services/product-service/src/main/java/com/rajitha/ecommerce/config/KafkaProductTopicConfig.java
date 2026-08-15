package com.rajitha.ecommerce.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProductTopicConfig {
    @Bean
    public NewTopic stockTopic() {
        return TopicBuilder.name("stock-topic").build();
    }
}
