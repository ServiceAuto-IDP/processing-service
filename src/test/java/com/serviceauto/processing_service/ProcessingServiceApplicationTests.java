package com.serviceauto.processing_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.scheduler.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false"
})
class ProcessingServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
