package br.com.orders.domain.service;

import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MoneyCalculatorTest {
    
    @InjectMocks
    private MoneyCalculator moneyCalculator;
    
    private List<OrderItem> testItems;
    
    @BeforeEach
    void setUp() {
        testItems = List.of(
                OrderItem.create("PROD-001", "Product 1", new BigDecimal("10.50"), 2),
                OrderItem.create("PROD-002", "Product 2", new BigDecimal("25.00"), 1),
                OrderItem.create("PROD-003", "Product 3", new BigDecimal("5.75"), 3)
        );
    }
    
    @Test
    void shouldCalculateOrderTotalCorrectly() {
        // When
        BigDecimal total = moneyCalculator.calculateOrderTotal(testItems);
        
        // Then
        assertThat(total).isEqualTo(new BigDecimal("63.25"));
    }
    
    @Test
    void shouldCalculateOrderTotalWithZeroItems() {
        // When
        BigDecimal total = moneyCalculator.calculateOrderTotal(List.of());
        
        // Then
        assertThat(total).isEqualTo(BigDecimal.ZERO);
    }
    
    @Test
    void shouldCalculateAndUpdateOrder() {
        // Given
        Order order = Order.create("EXT-001", testItems, "CORR-001");
        
        // When
        Order updatedOrder = moneyCalculator.calculateAndUpdateOrder(order);
        
        // Then
        assertThat(updatedOrder.getTotalAmount()).isEqualTo(new BigDecimal("63.25"));
        assertThat(updatedOrder.getUpdatedAt()).isNotNull();
        assertThat(updatedOrder.getId()).isEqualTo(order.getId());
    }
    
    @Test
    void shouldHandleLargeQuantities() {
        // Given
        List<OrderItem> largeItems = List.of(
                OrderItem.create("PROD-001", "Product 1", new BigDecimal("0.01"), 100000)
        );
        
        // When
        BigDecimal total = moneyCalculator.calculateOrderTotal(largeItems);
        
        // Then
        assertThat(total).isEqualTo(new BigDecimal("1000.00"));
    }
}
