package com.serviceauto.processing_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

    @Bean
    DirectExchange requestCreatedExchange(ProcessingServiceProperties properties) {
        return new DirectExchange(properties.messaging().requestCreatedExchange(), true, false);
    }

    @Bean
    Queue requestCreatedQueue(ProcessingServiceProperties properties) {
        return new Queue(properties.messaging().requestCreatedQueue(), true);
    }

    @Bean
    Binding requestCreatedBinding(
            Queue requestCreatedQueue,
            DirectExchange requestCreatedExchange,
            ProcessingServiceProperties properties
    ) {
        return BindingBuilder.bind(requestCreatedQueue)
                .to(requestCreatedExchange)
                .with(properties.messaging().requestCreatedRoutingKey());
    }
}
