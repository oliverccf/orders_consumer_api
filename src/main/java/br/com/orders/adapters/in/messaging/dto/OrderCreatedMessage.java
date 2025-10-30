package br.com.orders.adapters.in.messaging.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderCreatedMessage {
    private String externalId;
    private List<OrderItemMessage> items;
    private String correlationId;
    
    @JsonCreator
    public OrderCreatedMessage(@JsonProperty("externalId") String externalId,
                              @JsonProperty("items") List<OrderItemMessage> items,
                              @JsonProperty("correlationId") String correlationId) {
        this.externalId = externalId;
        this.items = items;
        this.correlationId = correlationId;
    }
    
    @Data
    @Builder
    public static class OrderItemMessage {
        private String productId;
        private String productName;
        private BigDecimal unitPrice;
        private Integer quantity;
        
        @JsonCreator
        public OrderItemMessage(@JsonProperty("productId") String productId,
                               @JsonProperty("productName") String productName,
                               @JsonProperty("unitPrice") BigDecimal unitPrice,
                               @JsonProperty("quantity") Integer quantity) {
            this.productId = productId;
            this.productName = productName;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }
    }
}
