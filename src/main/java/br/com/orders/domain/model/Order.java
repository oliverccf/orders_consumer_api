package br.com.orders.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@With
@Document(collection = "orders")
@CompoundIndexes({
    @CompoundIndex(name = "status_updatedAt_idx", def = "{'status': 1, 'updatedAt': -1}"),
    @CompoundIndex(name = "externalId_idx", def = "{'externalId': 1}", unique = true)
})
public class Order {
    
    @Id
    private String id;
    
    private String externalId;
    private OrderStatus status;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String correlationId;
    
    @Version
    private Long version;
    
    public static Order create(final String externalId, final List<OrderItem> items, final String correlationId) {
        return Order.builder()
                .id(UUID.randomUUID().toString())
                .externalId(externalId)
                .status(OrderStatus.PROCESSING)
                .items(items)
                .totalAmount(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .correlationId(correlationId)
                .version(0L)
                .build();
    }

    public Order acknowledge() {
        return this.withStatus(OrderStatus.ACKNOWLEDGED)
                .withUpdatedAt(LocalDateTime.now());
    }
}
