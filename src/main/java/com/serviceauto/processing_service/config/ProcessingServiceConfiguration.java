package com.serviceauto.processing_service.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(ProcessingServiceProperties.class)
public class ProcessingServiceConfiguration {

    @Bean
    RestClient ioServiceRestClient(RestClient.Builder builder, ProcessingServiceProperties properties) {
        return builder
                .baseUrl(properties.ioService().baseUrl())
                .build();
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
