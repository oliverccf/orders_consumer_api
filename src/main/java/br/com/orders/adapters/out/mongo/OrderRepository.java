package br.com.orders.adapters.out.mongo;

import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    
    Optional<Order> findByExternalId(String externalId);
    
    Page<Order> findByStatusOrderByUpdatedAtDesc(OrderStatus status, Pageable pageable);
    
    @Query("{'status': ?0}")
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    default Order upsert(Order order) {
        Optional<Order> existingOrder = findByExternalId(order.getExternalId());
        
        if (existingOrder.isPresent()) {
            Order existing = existingOrder.get();
            // Update existing order with new data but preserve the original ID and version
            Order updatedOrder = order.withId(existing.getId()).withVersion(existing.getVersion());
            return save(updatedOrder);
        } else {
            return save(order);
        }
    }
}
