package br.com.orders.component;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class TestContainersTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withReuse(true);
    
    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management")
            .withReuse(true);
    
    @Test
    void testContainersAreRunning() {
        assertTrue(mongoDBContainer.isRunning());
        assertTrue(rabbitMQContainer.isRunning());
        System.out.println("MongoDB URL: " + mongoDBContainer.getReplicaSetUrl());
        System.out.println("RabbitMQ Host: " + rabbitMQContainer.getHost());
        System.out.println("RabbitMQ Port: " + rabbitMQContainer.getAmqpPort());
    }
}
