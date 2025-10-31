package br.com.orders.adapters.in.http.dto;

import br.com.orders.domain.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
public record OrderResponse (
     String id,
     String externalId,
     OrderStatus status,
     List<OrderItemResponse> items,
     BigDecimal totalAmount,
     LocalDateTime createdAt,
     LocalDateTime updatedAt,
     String correlationId,
     Long version
) {

}
