package br.com.orders.adapters.in.messaging;

import br.com.orders.adapters.in.messaging.dto.OrderCreatedMessage;
import br.com.orders.domain.service.CalculateOrderService;
import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderItem;
import br.com.orders.domain.model.OrderStatus;
import br.com.orders.adapters.in.messaging.mapper.OrderMessageMapper;
import br.com.orders.adapters.in.messaging.validation.JsonSchemaValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreatedListenerTest {
    
    @Mock
    private CalculateOrderService calculateOrderService;
    
    @Mock
    private OrderMessageMapper orderMessageMapper;
    
    @Mock
    private JsonSchemaValidator jsonSchemaValidator;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private OrderCreatedListener orderCreatedListener;
    
    private OrderCreatedMessage testMessage;
    private Order testOrder;
    
    @BeforeEach
    void setUp() throws Exception {
        testMessage = OrderCreatedMessage.builder()
                .externalId("EXT-001")
                .correlationId("CORR-001")
                .items(List.of(
                        OrderCreatedMessage.OrderItemMessage.builder()
                                .productId("PROD-001")
                                .productName("Product 1")
                                .unitPrice(new BigDecimal("10.50"))
                                .quantity(2)
                                .build()
                ))
                .build();
        
        List<OrderItem> items = List.of(
                OrderItem.create("PROD-001", "Product 1", new BigDecimal("10.50"), 2)
        );
        
        testOrder = Order.create("EXT-001", items, "CORR-001")
                .withTotalAmount(new BigDecimal("21.00"))
                .withStatus(OrderStatus.AVAILABLE_FOR_B);
        
        when(objectMapper.readValue(anyString(), eq(OrderCreatedMessage.class))).thenReturn(testMessage);
        when(orderMessageMapper.toDomain(testMessage)).thenReturn(testOrder);
    }
    
    @Test
    void shouldProcessOrderCreatedMessageSuccessfully() throws Exception {
        // Given
        String messageBody = "{\"externalId\":\"EXT-001\",\"items\":[{\"productId\":\"PROD-001\",\"productName\":\"Product 1\",\"unitPrice\":10.50,\"quantity\":2}]}";
        Message message = createMessage(messageBody, "CORR-001", "MSG-001");
        
        when(calculateOrderService.processOrder(testOrder)).thenReturn(testOrder);
        
        // When
        orderCreatedListener.handleOrderCreated(message);
        
        // Then
        verify(jsonSchemaValidator).validateOrderCreated(messageBody);
        verify(objectMapper).readValue(messageBody, OrderCreatedMessage.class);
        verify(orderMessageMapper).toDomain(testMessage);
        verify(calculateOrderService).processOrder(testOrder);
    }
    
    @Test
    void shouldHandleProcessingException() throws Exception {
        // Given
        String messageBody = "{\"externalId\":\"EXT-001\",\"items\":[{\"productId\":\"PROD-001\",\"productName\":\"Product 1\",\"unitPrice\":10.50,\"quantity\":2}]}";
        Message message = createMessage(messageBody, "CORR-001", "MSG-001");
        
        when(calculateOrderService.processOrder(any(Order.class)))
                .thenThrow(new RuntimeException("Processing failed"));
        
        // When & Then
        try {
            orderCreatedListener.handleOrderCreated(message);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Failed to process order message");
        }
        
        verify(jsonSchemaValidator).validateOrderCreated(messageBody);
        verify(calculateOrderService).processOrder(testOrder);
    }
    
    private Message createMessage(String body, String correlationId, String messageId) {
        MessageProperties properties = new MessageProperties();
        properties.setCorrelationId(correlationId);
        properties.setMessageId(messageId);
        return new Message(body.getBytes(), properties);
    }
}
