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
    
    Optional<Order> findByExternalId(final String externalId);
    
    Page<Order> findByStatusOrderByUpdatedAtDesc(final OrderStatus status, final Pageable pageable);
    
    @Query("{'status': ?0}")
    Page<Order> findByStatus(final OrderStatus status, final Pageable pageable);
    
    default Order upsert(final Order order) {
        Optional<Order> existingOrder = findByExternalId(order.getExternalId());
        
        if (existingOrder.isPresent()) {
            var existing = existingOrder.get();
            var updatedOrder = order.withId(existing.getId()).withVersion(existing.getVersion());
            return save(updatedOrder);
        } else {
            return save(order);
        }
    }
}
