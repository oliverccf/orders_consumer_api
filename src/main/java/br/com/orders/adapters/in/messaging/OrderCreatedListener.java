package br.com.orders.adapters.in.messaging;

import br.com.orders.adapters.in.messaging.dto.OrderCreatedMessage;
import br.com.orders.domain.service.CalculateOrderService;
import br.com.orders.domain.model.Order;
import br.com.orders.adapters.in.messaging.mapper.OrderMessageMapper;
import br.com.orders.adapters.in.messaging.validation.JsonSchemaValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedListener {
    
    private final CalculateOrderService calculateOrderService;
    private final OrderMessageMapper orderMessageMapper;
    private final JsonSchemaValidator jsonSchemaValidator;
    private final ObjectMapper objectMapper;
    
    @RabbitListener(queues = "${app.rabbitmq.queues.incoming}")
    public void handleOrderCreated(Message message) {
        String correlationId = message.getMessageProperties().getCorrelationId();
        String orderId = message.getMessageProperties().getMessageId();
        
        // Set MDC for structured logging
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", orderId);
        
        try {
            log.info("Received order created message with correlationId: {}", correlationId);
            
            // Parse message
            String messageBody = new String(message.getBody());
            log.debug("Message body: {}", messageBody);
            
            // Handle double-encoding: if message body is a JSON-encoded string (when Jackson2JsonMessageConverter
            // serializes a String, it creates a JSON string value), parse it once to extract the actual JSON string
            String trimmedBody = messageBody.trim();
            if (trimmedBody.startsWith("\"") && trimmedBody.endsWith("\"")) {
                try {
                    // Try to parse as a JSON string value
                    String decoded = objectMapper.readValue(messageBody, String.class);
                    // If decoding succeeded, verify the result is valid JSON (should start with { or [)
                    if (decoded.trim().startsWith("{") || decoded.trim().startsWith("[")) {
                        messageBody = decoded;
                        log.debug("Decoded double-encoded JSON, new body: {}", messageBody);
                    }
                } catch (Exception e) {
                    // If parsing fails, assume the message body is already properly formatted JSON
                    log.debug("Failed to decode as JSON string, assuming message is already properly formatted: {}", e.getMessage());
                }
            }
            
            // Validate JSON schema
            jsonSchemaValidator.validateOrderCreated(messageBody);
            
            // Map to domain object
            OrderCreatedMessage orderMessage = objectMapper.readValue(messageBody, OrderCreatedMessage.class);
            Order order = orderMessageMapper.toDomain(orderMessage);
            
            // Process order
            Order processedOrder = calculateOrderService.processOrder(order);
            
            log.info("Successfully processed order: {} with total: {}", 
                    processedOrder.getId(), processedOrder.getTotalAmount());
            
        } catch (Exception e) {
            log.error("Error processing order created message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process order message", e);
        } finally {
            // Clear MDC
            MDC.clear();
        }
    }
}
