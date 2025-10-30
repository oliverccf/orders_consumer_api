package br.com.orders.adapters.in.http;

import br.com.orders.adapters.in.http.dto.OrderResponse;
import br.com.orders.domain.service.AckOrderService;
import br.com.orders.domain.service.ListOrdersService;
import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderItem;
import br.com.orders.domain.model.OrderStatus;
import br.com.orders.adapters.in.http.mapper.OrderResponseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private ListOrdersService listOrdersService;
    
    @MockitoBean
    private AckOrderService ackOrderService;
    
    @MockitoBean
    private OrderResponseMapper orderResponseMapper;
    
    private Order testOrder;
    private OrderResponse testOrderResponse;
    
    @BeforeEach
    void setUp() {
        List<OrderItem> items = List.of(
                OrderItem.create("PROD-001", "Product 1", new BigDecimal("10.50"), 2)
        );
        
        testOrder = Order.create("EXT-001", items, "CORR-001")
                .withId("ORDER-001")
                .withTotalAmount(new BigDecimal("21.00"))
                .withStatus(OrderStatus.AVAILABLE_FOR_B)
                .withCreatedAt(LocalDateTime.now())
                .withUpdatedAt(LocalDateTime.now())
                .withVersion(1L);
        
        testOrderResponse = OrderResponse.builder()
                .id("ORDER-001")
                .externalId("EXT-001")
                .status(OrderStatus.AVAILABLE_FOR_B)
                .totalAmount(new BigDecimal("21.00"))
                .correlationId("CORR-001")
                .version(1L)
                .build();
    }
    
    @Test
    @WithMockUser(authorities = "SCOPE_orders:read")
    void shouldListOrdersSuccessfully() throws Exception {
        // Given
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder), PageRequest.of(0, 20), 1);
        Page<OrderResponse> responsePage = new PageImpl<>(List.of(testOrderResponse), PageRequest.of(0, 20), 1);
        
        when(listOrdersService.listOrdersByStatus(eq(OrderStatus.AVAILABLE_FOR_B), any()))
                .thenReturn(orderPage);
        when(orderResponseMapper.toResponse(testOrder)).thenReturn(testOrderResponse);
        
        // When & Then
        mockMvc.perform(get("/orders")
                        .param("status", "AVAILABLE_FOR_B")
                        .with(jwt().authorities(() -> "SCOPE_orders:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("ORDER-001"))
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE_FOR_B"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
    
    @Test
    @WithMockUser(authorities = "SCOPE_orders:read")
    void shouldGetOrderByIdSuccessfully() throws Exception {
        // Given
        when(listOrdersService.findOrderById("ORDER-001")).thenReturn(Optional.of(testOrder));
        when(orderResponseMapper.toResponse(testOrder)).thenReturn(testOrderResponse);
        
        // When & Then
        mockMvc.perform(get("/orders/ORDER-001")
                        .with(jwt().authorities(() -> "SCOPE_orders:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ORDER-001"))
                .andExpect(jsonPath("$.status").value("AVAILABLE_FOR_B"));
    }
    
    @Test
    @WithMockUser(authorities = "SCOPE_orders:read")
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        // Given
        when(listOrdersService.findOrderById("NON-EXISTENT")).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/orders/NON-EXISTENT")
                        .with(jwt().authorities(() -> "SCOPE_orders:read")))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser(authorities = "SCOPE_orders:ack")
    void shouldAcknowledgeOrderSuccessfully() throws Exception {
        // Given
        Order acknowledgedOrder = testOrder.withStatus(OrderStatus.ACKNOWLEDGED);
        OrderResponse acknowledgedResponse = testOrderResponse.toBuilder()
                .status(OrderStatus.ACKNOWLEDGED)
                .build();
        
        when(ackOrderService.acknowledgeOrder("ORDER-001", 1L)).thenReturn(acknowledgedOrder);
        when(orderResponseMapper.toResponse(acknowledgedOrder)).thenReturn(acknowledgedResponse);
        
        // When & Then
        mockMvc.perform(post("/orders/ORDER-001/ack")
                        .header("If-Match", "1")
                        .with(jwt().authorities(() -> "SCOPE_orders:ack")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ORDER-001"))
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }
    
    @Test
    @WithMockUser(authorities = "SCOPE_orders:ack")
    void shouldReturnConflictOnVersionMismatch() throws Exception {
        // Given
        when(ackOrderService.acknowledgeOrder("ORDER-001", 1L))
                .thenThrow(new AckOrderService.OptimisticLockingException("Version mismatch"));
        
        // When & Then
        mockMvc.perform(post("/orders/ORDER-001/ack")
                        .header("If-Match", "1")
                        .with(jwt().authorities(() -> "SCOPE_orders:ack")))
                .andExpect(status().isConflict());
    }
    
    @Test
    void shouldReturnUnauthorizedWithoutToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized());
    }
}
