package br.com.orders.domain.service;

import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class MoneyCalculator {
    
    public BigDecimal calculateOrderTotal(List<OrderItem> items) {
        log.debug("Calculating total for {} items", items.size());
        
        BigDecimal total = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.debug("Calculated total: {}", total);
        return total;
    }
    
    public Order calculateAndUpdateOrder(Order order) {
        log.debug("Calculating total for order: {}", order.getId());
        
        BigDecimal total = calculateOrderTotal(order.getItems());
        
        Order updatedOrder = order.withTotalAmount(total)
                .withUpdatedAt(java.time.LocalDateTime.now());
        
        log.debug("Updated order {} with total: {}", order.getId(), total);
        return updatedOrder;
    }
}
