package br.com.orders.adapters.in.http.mapper;

import br.com.orders.adapters.in.http.dto.OrderItemResponse;
import br.com.orders.adapters.in.http.dto.OrderResponse;
import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper(componentModel = "spring")
@Component
public interface OrderResponseMapper {
    
    OrderResponseMapper INSTANCE = Mappers.getMapper(OrderResponseMapper.class);
    
    OrderResponse toResponse(Order order);
    
    List<OrderItemResponse> mapItems(List<OrderItem> items);
    
    OrderItemResponse mapItem(OrderItem item);
}
