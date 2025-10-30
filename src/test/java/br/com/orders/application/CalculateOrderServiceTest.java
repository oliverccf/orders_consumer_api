package br.com.orders.application;

import br.com.orders.adapters.out.mongo.OrderRepository;
import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderItem;
import br.com.orders.domain.model.OrderStatus;
import br.com.orders.domain.service.CalculateOrderService;
import br.com.orders.domain.service.MoneyCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculateOrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private MoneyCalculator moneyCalculator;
    
    @InjectMocks
    private CalculateOrderService calculateOrderService;
    
    private Order testOrder;
    
    @BeforeEach
    void setUp() {
        List<OrderItem> items = List.of(
                OrderItem.create("PROD-001", "Product 1", new BigDecimal("10.50"), 2)
        );
        
        testOrder = Order.create("EXT-001", items, "CORR-001");
    }
    
    @Test
    void shouldProcessOrderSuccessfully() {
        // Given
        Order calculatedOrder = testOrder.withTotalAmount(new BigDecimal("21.00"));
        Order savedOrder = calculatedOrder.withId("ORDER-001").withStatus(OrderStatus.AVAILABLE_FOR_B);
        
        when(moneyCalculator.calculateAndUpdateOrder(testOrder)).thenReturn(calculatedOrder);
        when(orderRepository.upsert(any(Order.class))).thenReturn(savedOrder);
        
        // When
        Order result = calculateOrderService.processOrder(testOrder);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("ORDER-001");
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("21.00"));
        assertThat(result.getStatus()).isEqualTo(OrderStatus.AVAILABLE_FOR_B);
    }
    
    @Test
    void shouldHandleProcessingException() {
        // Given
        when(moneyCalculator.calculateAndUpdateOrder(testOrder))
                .thenThrow(new RuntimeException("Calculation failed"));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        
        // When & Then
        assertThatThrownBy(() -> calculateOrderService.processOrder(testOrder))
                .isInstanceOf(CalculateOrderService.OrderProcessingException.class)
                .hasMessageContaining("Failed to process order");
    }
}
