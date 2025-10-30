package br.com.orders.adapters.out.mongo;

import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderItem;
import br.com.orders.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
class OrderRepositoryTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withReuse(true);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private OrderRepository orderRepository;
    
    private Order testOrder;
    
    @BeforeEach
    void setUp() {
        // Clear all data before each test
        orderRepository.deleteAll();
        
        List<OrderItem> items = List.of(
                OrderItem.create("PROD-001", "Product 1", new BigDecimal("10.50"), 2)
        );
        
        // Create order without version to avoid optimistic locking issues
        testOrder = Order.create("EXT-001", items, "CORR-001")
                .withTotalAmount(new BigDecimal("21.00"))
                .withStatus(OrderStatus.AVAILABLE_FOR_B)
                .withVersion(null); // Ensure version is null for new entities
    }
    
    @Test
    void shouldSaveAndFindOrderById() {
        // When
        Order savedOrder = orderRepository.save(testOrder);
        Optional<Order> foundOrder = orderRepository.findById(savedOrder.getId());
        
        // Then
        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getExternalId()).isEqualTo("EXT-001");
        assertThat(foundOrder.get().getStatus()).isEqualTo(OrderStatus.AVAILABLE_FOR_B);
    }
    
    @Test
    void shouldFindOrderByExternalId() {
        // When
        orderRepository.save(testOrder);
        Optional<Order> foundOrder = orderRepository.findByExternalId("EXT-001");
        
        // Then
        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getExternalId()).isEqualTo("EXT-001");
    }
    
    @Test
    void shouldFindOrdersByStatus() {
        // Given
        Order order1 = testOrder.withId("ORDER-001").withVersion(null);
        Order order2 = testOrder.withId("ORDER-002").withExternalId("EXT-002").withVersion(null);
        
        orderRepository.save(order1);
        orderRepository.save(order2);
        
        // When
        Page<Order> orders = orderRepository.findByStatusOrderByUpdatedAtDesc(
                OrderStatus.AVAILABLE_FOR_B, PageRequest.of(0, 10));
        
        // Then
        assertThat(orders.getTotalElements()).isEqualTo(2);
        assertThat(orders.getContent()).hasSize(2);
    }
    
    @Test
    void shouldUpsertOrder() {
        // Given
        Order savedOrder = orderRepository.save(testOrder);
        
        // When - Update with same external ID
        Order updatedOrder = testOrder.withTotalAmount(new BigDecimal("25.00")).withVersion(null);
        Order upsertedOrder = orderRepository.upsert(updatedOrder);
        
        // Then
        assertThat(upsertedOrder.getId()).isEqualTo(savedOrder.getId());
        assertThat(upsertedOrder.getTotalAmount()).isEqualTo(new BigDecimal("25.00"));
        
        // Verify only one order exists
        assertThat(orderRepository.count()).isEqualTo(1);
    }
}
