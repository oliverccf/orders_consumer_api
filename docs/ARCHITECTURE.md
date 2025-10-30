# Order Service Architecture Documentation

## System Overview

The Order Service is a microservice designed to process orders from external systems, calculate totals, and make them available for consumption by other external systems. It implements hexagonal architecture principles and is built to handle high-volume order processing (150,000-200,000 orders per day).

## Architecture Diagram

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Product A     │    │  Order Service  │    │   Product B     │
│  (External)     │    │                 │    │  (External)     │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │Order Created│ │───▶│ │RabbitMQ     │ │    │ │REST API     │ │
│ │Message      │ │    │ │Listener     │ │    │ │Consumer     │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
│                 │    │                 │    │                 │
│                 │    │ ┌─────────────┐ │    │                 │
│                 │    │ │Order        │ │    │                 │
│                 │    │ │Processing   │ │    │                 │
│                 │    │ │Service      │ │    │                 │
│                 │    │ └─────────────┘ │    │                 │
│                 │    │                 │    │                 │
│                 │    │ ┌─────────────┐ │    │                 │
│                 │    │ │MongoDB      │ │    │                 │
│                 │    │ │Repository   │ │    │                 │
│                 │    │ └─────────────┘ │    │                 │
│                 │    │                 │    │                 │
│                 │    │ ┌─────────────┐ │◀───│                 │
│                 │    │ │REST API     │ │    │                 │
│                 │    │ │Controller   │ │    │                 │
│                 │    │ └─────────────┘ │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Hexagonal Architecture Layers

### Domain Layer
**Location**: `br.com.orders.domain`

Contains pure business logic and domain models:

- **Models**: `Order`, `OrderItem`, `OrderStatus`
- **Services**: `MoneyCalculator` - Pure calculation logic
- **No dependencies** on external frameworks

### Application Layer
**Location**: `br.com.orders.application`

Orchestrates use cases and coordinates between domain and adapters:

- **Services**: `CalculateOrderService`, `ListOrdersService`, `AckOrderService`
- **Use Cases**: Order processing, listing, acknowledgment
- **Dependencies**: Domain layer only

### Adapters Layer
**Location**: `br.com.orders.adapters`

Handles external integrations:

#### Inbound Adapters (Primary)
- **HTTP**: `OrderController` - REST API endpoints
- **Messaging**: `OrderCreatedListener` - RabbitMQ message consumption

#### Outbound Adapters (Secondary)
- **Database**: `OrderRepository` - MongoDB persistence
- **Messaging**: RabbitMQ configuration

## Data Flow

### 1. Order Creation Flow
```
Product A → RabbitMQ → OrderCreatedListener → CalculateOrderService → MongoDB
```

1. **Product A** sends order message to RabbitMQ
2. **OrderCreatedListener** consumes message
3. **JSON Schema validation** ensures message integrity
4. **CalculateOrderService** processes order and calculates total
5. **OrderRepository** persists order with `AVAILABLE_FOR_B` status

### 2. Order Consumption Flow
```
Product B → REST API → OrderController → ListOrdersService/AckOrderService → MongoDB
```

1. **Product B** calls REST API endpoints
2. **OrderController** handles HTTP requests
3. **ListOrdersService** retrieves orders by status
4. **AckOrderService** acknowledges orders with optimistic locking
5. **OrderRepository** updates order status

## Technology Stack

### Core Framework
- **Spring Boot 3.5+** - Application framework
- **Java 21** - Programming language with modern features
- **Maven** - Build and dependency management

### Data Layer
- **MongoDB 7.0** - Document database with replica set
- **Spring Data MongoDB** - Data access abstraction
- **MongoDB indexes** for performance optimization

### Messaging
- **RabbitMQ 3.13** - Message broker
- **Spring AMQP** - Messaging abstraction
- **Dead Letter Queue** for error handling

### Security
- **Spring Security** - Authentication and authorization
- **OAuth2 Resource Server** - JWT token validation
- **Scoped permissions** for fine-grained access control

### Testing
- **JUnit 5** - Unit testing framework
- **Testcontainers** - Integration testing with real services
- **Cucumber** - Behavior-driven component testing
- **Mockito** - Mocking framework
- **Awaitility** - Asynchronous testing

### Observability
- **Spring Actuator** - Health checks and metrics
- **New Relic** - Application performance monitoring
- **Structured logging** with correlation IDs
- **Prometheus metrics** export

## Design Patterns

### Hexagonal Architecture (Ports & Adapters)
- **Ports**: Interfaces defining contracts
- **Adapters**: Implementations of external integrations
- **Dependency Inversion**: Core business logic independent of external concerns

### Domain-Driven Design (DDD)
- **Aggregates**: Order as the main aggregate
- **Value Objects**: OrderItem, Money calculations
- **Domain Services**: Pure business logic
- **Repositories**: Data access abstraction

### SOLID Principles
- **Single Responsibility**: Each class has one reason to change
- **Open/Closed**: Open for extension, closed for modification
- **Liskov Substitution**: Subtypes must be substitutable for base types
- **Interface Segregation**: Clients depend only on interfaces they use
- **Dependency Inversion**: Depend on abstractions, not concretions

## Scalability Considerations

### Horizontal Scaling
- **Stateless design** - No session state
- **Container orchestration** - Kubernetes/Docker Swarm ready
- **Load balancing** - Multiple instances behind load balancer
- **Database sharding** - MongoDB sharding for large datasets

### Performance Optimization
- **Connection pooling** - Database and messaging connections
- **Async processing** - Non-blocking message consumption
- **Caching** - Redis integration ready
- **Index optimization** - MongoDB compound indexes

### High Availability
- **Health checks** - Kubernetes liveness/readiness probes
- **Circuit breakers** - Resilience patterns
- **Retry mechanisms** - Exponential backoff
- **Dead letter queues** - Error handling and recovery

## Security Architecture

### Authentication
- **JWT tokens** - Stateless authentication
- **OAuth2 Resource Server** - Token validation
- **Configurable issuers** - Multiple identity providers

### Authorization
- **Scoped permissions** - Fine-grained access control
- **Method-level security** - `@PreAuthorize` annotations
- **Role-based access** - Extensible permission model

### Data Protection
- **Input validation** - JSON Schema validation
- **SQL injection prevention** - MongoDB NoSQL
- **CORS configuration** - Cross-origin resource sharing
- **Sensitive data handling** - No PII in logs

## Monitoring and Observability

### Logging
- **Structured JSON logs** - Machine-readable format
- **Correlation IDs** - Request tracing across services
- **MDC integration** - Thread-local context
- **Log levels** - Configurable verbosity

### Metrics
- **Business metrics** - Order processing rates
- **Technical metrics** - Response times, error rates
- **Custom metrics** - Domain-specific measurements
- **Prometheus export** - Standard metrics format

### Tracing
- **Distributed tracing** - Request flow across services
- **New Relic integration** - APM and error tracking
- **Custom spans** - Business operation tracing
- **Performance monitoring** - Bottleneck identification

## Deployment Architecture

### Container Strategy
- **Multi-stage builds** - Optimized Docker images
- **Non-root users** - Security best practices
- **Health checks** - Container orchestration integration
- **Resource limits** - CPU and memory constraints

### Environment Configuration
- **12-factor app** - Environment-based configuration
- **Secrets management** - External secret stores
- **Feature flags** - Runtime configuration
- **Environment profiles** - Dev/staging/production

### Infrastructure as Code
- **Docker Compose** - Local development
- **Kubernetes manifests** - Production deployment
- **Helm charts** - Package management
- **CI/CD pipelines** - Automated deployment

## Error Handling Strategy

### Message Processing Errors
- **Dead letter queues** - Failed message handling
- **Retry mechanisms** - Transient error recovery
- **Circuit breakers** - Cascade failure prevention
- **Error notifications** - Alert systems

### API Errors
- **HTTP status codes** - Standard error responses
- **Error details** - Structured error information
- **Validation errors** - Input validation feedback
- **Rate limiting** - API protection

### Database Errors
- **Connection failures** - Retry with backoff
- **Optimistic locking** - Concurrent access handling
- **Transaction management** - Data consistency
- **Index optimization** - Performance tuning

## Future Enhancements

### Performance Improvements
- **Event sourcing** - Complete audit trail
- **CQRS** - Command Query Responsibility Segregation
- **Caching layer** - Redis integration
- **Async processing** - Virtual threads (Java 21)

### Feature Additions
- **Order history** - Complete order lifecycle
- **Bulk operations** - Batch processing
- **Webhooks** - Real-time notifications
- **GraphQL API** - Flexible data querying

### Operational Improvements
- **Chaos engineering** - Resilience testing
- **A/B testing** - Feature experimentation
- **Blue-green deployment** - Zero-downtime updates
- **Automated scaling** - Dynamic resource allocation
