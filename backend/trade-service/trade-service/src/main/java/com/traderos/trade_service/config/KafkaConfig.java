package com.traderos.trade_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

@Configuration
public class KafkaConfig {

    // This bean intercepts incoming Kafka messages and automatically
    // converts the JSON String payload into your Java Record.
    @Bean
    public RecordMessageConverter converter() {
        return new StringJsonMessageConverter();
    }
}