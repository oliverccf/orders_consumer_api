package br.com.orders.domain.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItem {
    
    private String productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalPrice;
    
    public static OrderItem create(String productId, String productName, BigDecimal unitPrice, Integer quantity) {
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        
        return OrderItem.builder()
                .productId(productId)
                .productName(productName)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .build();
    }
}
