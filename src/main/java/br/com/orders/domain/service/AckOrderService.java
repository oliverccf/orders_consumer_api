package br.com.orders.domain.service;

import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderStatus;
import br.com.orders.adapters.out.mongo.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AckOrderService {
    
    private final OrderRepository orderRepository;
    
    @Transactional
    public Order acknowledgeOrder(String orderId, Long expectedVersion) {
        log.info("Acknowledging order: {} with expected version: {}", orderId, expectedVersion);
        
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        
        if (orderOpt.isEmpty()) {
            log.warn("Order not found for acknowledgment: {}", orderId);
            throw new OrderNotFoundException("Order not found: " + orderId);
        }
        
        Order order = orderOpt.get();
        
        // Check if order is in correct status
        if (order.getStatus() != OrderStatus.AVAILABLE_FOR_B) {
            log.warn("Order {} is not available for acknowledgment. Current status: {}", 
                    orderId, order.getStatus());
            throw new InvalidOrderStatusException("Order is not available for acknowledgment. Current status: " + order.getStatus());
        }
        
        // Check version for optimistic locking
        if (!expectedVersion.equals(order.getVersion())) {
            log.warn("Version mismatch for order {}. Expected: {}, Actual: {}", 
                    orderId, expectedVersion, order.getVersion());
            throw new OptimisticLockingException("Version mismatch. Expected: " + expectedVersion + ", Actual: " + order.getVersion());
        }
        
        // Acknowledge the order
        Order acknowledgedOrder = order.acknowledge();
        Order savedOrder = orderRepository.save(acknowledgedOrder);
        
        log.info("Successfully acknowledged order: {}", orderId);
        return savedOrder;
    }
    
    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) {
            super(message);
        }
    }
    
    public static class InvalidOrderStatusException extends RuntimeException {
        public InvalidOrderStatusException(String message) {
            super(message);
        }
    }
    
    public static class OptimisticLockingException extends RuntimeException {
        public OptimisticLockingException(String message) {
            super(message);
        }
    }
}
