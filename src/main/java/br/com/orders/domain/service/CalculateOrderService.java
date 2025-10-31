package br.com.orders.domain.service;

import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderStatus;
import br.com.orders.adapters.out.mongo.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalculateOrderService {
    
    private final OrderRepository orderRepository;
    private final MoneyCalculator moneyCalculator;
    
    @Transactional
    public Order processOrder(final Order order) {
        log.info("Processing order: {} with externalId: {}", order.getId(), order.getExternalId());
        
        try {
            // Calculate total using domain service
            var calculatedOrder = moneyCalculator.calculateAndUpdateOrder(order);
            
            // Update status to available for Product B
            var processedOrder = calculatedOrder.withStatus(OrderStatus.AVAILABLE_FOR_B);
            
            // Save with upsert to handle idempotency
            var savedOrder = orderRepository.upsert(processedOrder);
            
            log.info("Successfully processed order: {} with total: {}", 
                    savedOrder.getId(), savedOrder.getTotalAmount());
            
            return savedOrder;
            
        } catch (Exception e) {
            log.error("Error processing order: {} - {}", order.getId(), e.getMessage(), e);
            
            // Mark order as failed
            var failedOrder = order.withStatus(OrderStatus.FAILED);
            orderRepository.save(failedOrder);
            
            throw new OrderProcessingException("Failed to process order: " + order.getId(), e);
        }
    }
    
    public static class OrderProcessingException extends RuntimeException {
        public OrderProcessingException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
