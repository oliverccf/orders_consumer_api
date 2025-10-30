package br.com.orders.adapters.in.messaging.mapper;

import br.com.orders.adapters.in.messaging.dto.OrderCreatedMessage;
import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper(componentModel = "spring")
@Component
public interface OrderMessageMapper {
    
    OrderMessageMapper INSTANCE = Mappers.getMapper(OrderMessageMapper.class);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Order toDomain(OrderCreatedMessage message);
    
    List<OrderItem> mapItems(List<OrderCreatedMessage.OrderItemMessage> items);
    
    @Mapping(target = "totalPrice", expression = "java(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))")
    OrderItem mapItem(OrderCreatedMessage.OrderItemMessage item);
}
