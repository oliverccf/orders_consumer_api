package br.com.orders.adapters.in.http;

import br.com.orders.adapters.in.http.dto.OrderResponse;
import br.com.orders.adapters.in.http.mapper.OrderResponseMapper;
import br.com.orders.domain.service.AckOrderService;
import br.com.orders.domain.service.ListOrdersService;
import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management API")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {
    
    private final ListOrdersService listOrdersService;
    private final AckOrderService ackOrderService;
    private final OrderResponseMapper orderResponseMapper;
    
    @GetMapping
    // @PreAuthorize("hasAuthority('SCOPE_orders:read')")
    @Operation(summary = "List orders by status", description = "Retrieve orders filtered by status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @Parameter(description = "Order status filter") 
            @RequestParam(defaultValue = "AVAILABLE_FOR_B") OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.debug("Listing orders with status: {} and page: {}", status, pageable);
        
        Page<Order> orders = listOrdersService.listOrdersByStatus(status, pageable);
        Page<OrderResponse> response = orders.map(orderResponseMapper::toResponse);
        
        log.debug("Returning {} orders", response.getTotalElements());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    // @PreAuthorize("hasAuthority('SCOPE_orders:read')")
    @Operation(summary = "Get order by ID", description = "Retrieve a specific order by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Order ID") 
            @PathVariable String id) {
        
        log.debug("Getting order by id: {}", id);
        
        Optional<Order> order = listOrdersService.findOrderById(id);
        
        if (order.isEmpty()) {
            log.debug("Order not found: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        OrderResponse response = orderResponseMapper.toResponse(order.get());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/ack")
    // @PreAuthorize("hasAuthority('SCOPE_orders:ack')")
    @Operation(summary = "Acknowledge order", description = "Acknowledge receipt of an order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order acknowledged successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "409", description = "Version conflict"),
        @ApiResponse(responseCode = "400", description = "Invalid order status"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<OrderResponse> acknowledgeOrder(
            @Parameter(description = "Order ID") 
            @PathVariable String id,
            @Parameter(description = "Expected version for optimistic locking") 
            @RequestHeader("If-Match") Long expectedVersion) {
        
        log.info("Acknowledging order: {} with version: {}", id, expectedVersion);
        
        try {
            Order acknowledgedOrder = ackOrderService.acknowledgeOrder(id, expectedVersion);
            OrderResponse response = orderResponseMapper.toResponse(acknowledgedOrder);
            
            log.info("Successfully acknowledged order: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (AckOrderService.OrderNotFoundException e) {
            log.warn("Order not found for acknowledgment: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (AckOrderService.InvalidOrderStatusException e) {
            log.warn("Invalid order status for acknowledgment: {} - {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            
        } catch (AckOrderService.OptimisticLockingException e) {
            log.warn("Version conflict for order: {} - {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
