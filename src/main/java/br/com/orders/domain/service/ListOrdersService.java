package br.com.orders.domain.service;

import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderStatus;
import br.com.orders.adapters.out.mongo.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListOrdersService {
    
    private final OrderRepository orderRepository;
    
    public Page<Order> listOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.debug("Listing orders with status: {} and page: {}", status, pageable);
        
        Page<Order> orders = orderRepository.findByStatusOrderByUpdatedAtDesc(status, pageable);
        
        log.debug("Found {} orders with status: {}", orders.getTotalElements(), status);
        return orders;
    }
    
    public Optional<Order> findOrderById(String orderId) {
        log.debug("Finding order by id: {}", orderId);
        
        Optional<Order> order = orderRepository.findById(orderId);
        
        if (order.isPresent()) {
            log.debug("Found order: {} with status: {}", orderId, order.get().getStatus());
        } else {
            log.debug("Order not found: {}", orderId);
        }
        
        return order;
    }
    
    public Optional<Order> findOrderByExternalId(String externalId) {
        log.debug("Finding order by external id: {}", externalId);
        
        Optional<Order> order = orderRepository.findByExternalId(externalId);
        
        if (order.isPresent()) {
            log.debug("Found order: {} with external id: {}", order.get().getId(), externalId);
        } else {
            log.debug("Order not found with external id: {}", externalId);
        }
        
        return order;
    }
}
