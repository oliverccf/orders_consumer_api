package br.com.orders.component;

import br.com.orders.adapters.in.http.dto.OrderResponse;
import br.com.orders.domain.model.Order;
import br.com.orders.domain.model.OrderItem;
import br.com.orders.domain.model.OrderStatus;
import br.com.orders.adapters.out.mongo.OrderRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import br.com.orders.config.TestSecurityConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.awaitility.Awaitility;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
public class OrderProcessingSteps extends ComponentTestBase {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    private String externalId;
    private String correlationId;
    private String authToken;
    private OrderResponse apiOrderResponse;
    private Order databaseOrder;
    
    @Given("an order with external ID {string} and correlation ID {string}")
    public void anOrderWithExternalIdAndCorrelationId(String externalId, String correlationId) {
        this.externalId = externalId;
        this.correlationId = correlationId;
    }
    
    @When("the order message is sent to the incoming queue")
    public void theOrderMessageIsSentToTheIncomingQueue() {
        String messageBody = String.format("""
                {
                    "externalId": "%s",
                    "correlationId": "%s",
                    "items": [
                        {
                            "productId": "PROD-001",
                            "productName": "Test Product",
                            "unitPrice": 10.50,
                            "quantity": 2
                        },
                        {
                            "productId": "PROD-002",
                            "productName": "Another Product",
                            "unitPrice": 5.25,
                            "quantity": 1
                        }
                    ]
                }
                """, externalId, correlationId);
        
        rabbitTemplate.convertAndSend("orders.incoming.ex", "order.created", messageBody);
    }
    
    @Then("the order should be processed and saved in the database")
    public void theOrderShouldBeProcessedAndSavedInTheDatabase() {
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Optional<Order> order = orderRepository.findByExternalId(externalId);
                    assertThat(order).isPresent();
                    
                    Order savedOrder = order.get();
                    assertThat(savedOrder.getExternalId()).isEqualTo(externalId);
                    assertThat(savedOrder.getCorrelationId()).isEqualTo(correlationId);
                    assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.AVAILABLE_FOR_B);
                    assertThat(savedOrder.getTotalAmount()).isEqualTo(new BigDecimal("26.25"));
                    assertThat(savedOrder.getItems()).hasSize(2);
                });
    }
    
    @Then("the order should have the correct total amount calculated")
    public void theOrderShouldHaveTheCorrectTotalAmountCalculated() {
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Optional<Order> order = orderRepository.findByExternalId(externalId);
                    assertThat(order).isPresent();
                    
                    Order savedOrder = order.get();
                    BigDecimal expectedTotal = new BigDecimal("26.25"); // 10.50 * 2 + 5.25 * 1
                    assertThat(savedOrder.getTotalAmount()).isEqualTo(expectedTotal);
                });
    }
    
    @Given("an existing order with external ID {string}")
    public void anExistingOrderWithExternalId(String externalId) {
        this.externalId = externalId;
        
        // Clean up any existing order with this external ID to avoid conflicts
        orderRepository.findByExternalId(externalId).ifPresent(order -> orderRepository.delete(order));
        
        List<OrderItem> items = List.of(
                OrderItem.create("PROD-001", "Test Product", new BigDecimal("10.50"), 2)
        );
        
        // Create order using builder directly without version to allow Spring Data MongoDB 
        // to manage versioning automatically. The Order.create() method sets version to 0L 
        // which can cause optimistic locking conflicts.
        Order order = Order.builder()
                .id(UUID.randomUUID().toString())
                .externalId(externalId)
                .items(items)
                .correlationId("CORR-001")
                .totalAmount(new BigDecimal("21.00"))
                .status(OrderStatus.AVAILABLE_FOR_B)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                // version is not set, so it will be null and Spring Data MongoDB will manage it
                .build();
        
        orderRepository.save(order);
    }
    
    @When("a duplicate order message is sent")
    public void aDuplicateOrderMessageIsSent() {
        String messageBody = String.format("""
                {
                    "externalId": "%s",
                    "correlationId": "CORR-002",
                    "items": [
                        {
                            "productId": "PROD-001",
                            "productName": "Test Product",
                            "unitPrice": 10.50,
                            "quantity": 2
                        }
                    ]
                }
                """, externalId);
        
        rabbitTemplate.convertAndSend("orders.incoming.ex", "order.created", messageBody);
    }
    
    @Then("the order should be updated with idempotency")
    public void theOrderShouldBeUpdatedWithIdempotency() {
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    // Verify that only one order exists with this externalId (idempotency)
                    // Count all orders with this externalId to ensure no duplicates were created
                    List<Order> ordersWithExternalId = orderRepository.findAll().stream()
                            .filter(order -> externalId.equals(order.getExternalId()))
                            .toList();
                    assertThat(ordersWithExternalId).hasSize(1); // Only one order with this externalId should exist
                    
                    // Verify the order was updated with the new correlation ID
                    Optional<Order> order = orderRepository.findByExternalId(externalId);
                    assertThat(order).isPresent();
                    assertThat(order.get().getCorrelationId()).isEqualTo("CORR-002"); // Updated correlation ID
                });
    }
    
    @And("I wait for the order to be processed")
    public void iWaitForTheOrderToBeProcessed() {
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Optional<Order> order = orderRepository.findByExternalId(externalId);
                    assertThat(order).isPresent();
                    assertThat(order.get().getStatus()).isEqualTo(OrderStatus.AVAILABLE_FOR_B);
                });
    }
    
    @And("I generate an authentication token")
    public void iGenerateAnAuthenticationToken() {
        authToken = TestTokenHelper.generateTestToken();
        assertThat(authToken).isNotNull();
        assertThat(authToken).isNotEmpty();
    }
    
    @When("I query the order via API using the token")
    public void iQueryTheOrderViaAPIUsingTheToken() {
        // First, get the order from database to get the internal ID
        Optional<Order> orderOpt = orderRepository.findByExternalId(externalId);
        assertThat(orderOpt).isPresent();
        databaseOrder = orderOpt.get();
        
        String orderId = databaseOrder.getId();
        String url = "http://localhost:" + port + "/api/v1/orders/" + orderId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                OrderResponse.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        apiOrderResponse = response.getBody();
        assertThat(apiOrderResponse).isNotNull();
    }
    
    @Then("the order should be retrieved successfully from the API")
    public void theOrderShouldBeRetrievedSuccessfullyFromTheAPI() {
        assertThat(apiOrderResponse).isNotNull();
        assertThat(apiOrderResponse.getId()).isEqualTo(databaseOrder.getId());
        assertThat(apiOrderResponse.getExternalId()).isEqualTo(externalId);
        assertThat(apiOrderResponse.getStatus()).isEqualTo(OrderStatus.AVAILABLE_FOR_B);
    }
    
    @And("the API response should match the order in the database")
    public void theAPIResponseShouldMatchTheOrderInTheDatabase() {
        assertThat(apiOrderResponse.getExternalId()).isEqualTo(databaseOrder.getExternalId());
        assertThat(apiOrderResponse.getCorrelationId()).isEqualTo(databaseOrder.getCorrelationId());
        assertThat(apiOrderResponse.getStatus()).isEqualTo(databaseOrder.getStatus());
        assertThat(apiOrderResponse.getTotalAmount()).isEqualByComparingTo(databaseOrder.getTotalAmount());
        assertThat(apiOrderResponse.getItems()).hasSize(databaseOrder.getItems().size());
        assertThat(apiOrderResponse.getVersion()).isEqualTo(databaseOrder.getVersion());
    }
}
