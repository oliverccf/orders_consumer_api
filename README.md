# Order Service - Order Management System

A comprehensive order management microservice built with Spring Boot 3.5+, Java 21, MongoDB, and RabbitMQ, implementing hexagonal architecture principles.

## 🎯 Overview

This service processes orders from external systems (Product A) via RabbitMQ, calculates totals, persists data in MongoDB, and provides REST APIs for order consumption by external systems (Product B).

## 🏗️ Architecture

### Hexagonal Architecture (Ports & Adapters)
- **Domain Layer**: Pure business logic and models
- **Application Layer**: Use cases and orchestration
- **Adapters**: External integrations (HTTP, Messaging, Database)

### Key Components
- **Order Processing**: Receives messages, validates, calculates totals
- **Idempotency**: Handles duplicate messages gracefully
- **Optimistic Locking**: Version control for concurrent access
- **Security**: JWT-based authentication with scoped permissions

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.8+

### Development Setup

1. **Clone and setup**:
   ```bash
   git clone <repository>
   cd AMQOrderConsumer
   ```

2. **Start infrastructure**:
   ```bash
   ./scripts/setup-dev.sh
   ```

3. **Access services**:
   - Application: http://localhost:8080
   - API Docs: http://localhost:8080/swagger-ui.html
   - RabbitMQ Management: http://localhost:15672 (guest/guest)

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Component tests (Cucumber)
mvn test -Dtest=*ComponentTest
```

## 📡 API Endpoints

### Authentication
All endpoints require JWT authentication with appropriate scopes:
- `SCOPE_orders:read` - For reading orders
- `SCOPE_orders:ack` - For acknowledging orders

### Order Management

| Method | Endpoint | Description | Scope |
|--------|----------|-------------|-------|
| GET | `/orders` | List orders by status | `orders:read` |
| GET | `/orders/{id}` | Get order by ID | `orders:read` |
| POST | `/orders/{id}/ack` | Acknowledge order | `orders:ack` |

### Example Usage

```bash
# List available orders
curl -H "Authorization: Bearer <jwt-token>" \
     "http://localhost:8080/api/v1/orders?status=AVAILABLE_FOR_B"

# Acknowledge an order
curl -X POST \
     -H "Authorization: Bearer <jwt-token>" \
     -H "If-Match: 1" \
     "http://localhost:8080/api/v1/orders/ORDER-001/ack"
```

## 🔄 Message Flow

### Order Creation (Product A → Order Service)

```json
{
  "externalId": "EXT-001",
  "correlationId": "CORR-001",
  "items": [
    {
      "productId": "PROD-001",
      "productName": "Product 1",
      "unitPrice": 10.50,
      "quantity": 2
    }
  ]
}
```

**Queue**: `orders.incoming.q`  
**Exchange**: `orders.incoming.ex`  
**Routing Key**: `order.created`

### Order Processing Flow

1. **Message Reception**: RabbitMQ listener receives order message
2. **Validation**: JSON Schema validation ensures message integrity
3. **Calculation**: Domain service calculates order total
4. **Persistence**: Order saved to MongoDB with `AVAILABLE_FOR_B` status
5. **Idempotency**: Duplicate messages handled via external ID

## 🗄️ Data Model

### Order Entity
```java
{
  "id": "uuid",
  "externalId": "EXT-001",
  "status": "AVAILABLE_FOR_B",
  "items": [...],
  "totalAmount": 21.00,
  "createdAt": "2024-01-01T10:00:00Z",
  "updatedAt": "2024-01-01T10:00:00Z",
  "correlationId": "CORR-001",
  "version": 1
}
```

### Order Statuses
- `PROCESSING` - Initial state during calculation
- `AVAILABLE_FOR_B` - Ready for external system consumption
- `ACKNOWLEDGED` - Confirmed by external system
- `FAILED` - Processing failed

## 🔒 Security

### JWT Configuration
- **Issuer**: Configurable via `JWT_ISSUER_URI`
- **JWK Set**: Configurable via `JWT_JWK_SET_URI`
- **Scopes**: Fine-grained permissions for different operations

### Authorization
```java
@PreAuthorize("hasAuthority('SCOPE_orders:read')")
@PreAuthorize("hasAuthority('SCOPE_orders:ack')")
```

## 📊 Observability

### Logging
- **Structured JSON logs** with correlation IDs
- **MDC integration** for request tracing
- **Logback configuration** with file rotation

### Metrics
- **Spring Actuator** endpoints
- **New Relic integration** ready
- **Custom metrics** for business operations

### Monitoring Endpoints
- `/actuator/health` - Health check
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

## 🧪 Testing Strategy

### Test Types
1. **Unit Tests**: Domain logic and services
2. **Integration Tests**: Repository and messaging
3. **Component Tests**: End-to-end with Cucumber
4. **Contract Tests**: API specifications

### Test Coverage
- **Minimum**: 80% code coverage (Jacoco)
- **Testcontainers**: Real MongoDB and RabbitMQ
- **Awaitility**: Asynchronous testing support

## 🐳 Docker Deployment

### Development
```bash
docker compose -f docker/docker-compose.dev.yml up -d
```

### Production Considerations
- **Resource limits** and health checks
- **Secrets management** for sensitive data
- **Log aggregation** and monitoring
- **Horizontal scaling** with load balancers

## 🔧 Configuration

### Environment Variables
```bash
# Database
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/orders

# Messaging
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672

# Security
JWT_ISSUER_URI=http://localhost:8080/auth/realms/orders
JWT_JWK_SET_URI=http://localhost:8080/auth/realms/orders/protocol/openid-connect/certs

# Observability
NEW_RELIC_LICENSE_KEY=your-license-key
```

## 📈 Performance & Scalability

### Design Considerations
- **Event-driven architecture** for loose coupling
- **MongoDB indexes** for efficient queries
- **Connection pooling** for database and messaging
- **Async processing** for high throughput

### Volume Handling
- **150,000-200,000 orders/day** capacity
- **Horizontal scaling** via container orchestration
- **Database sharding** for large datasets
- **Message partitioning** for parallel processing

## 🛠️ Development

### Code Quality
- **Clean Code** principles
- **SOLID** design patterns
- **MapStruct** for object mapping
- **Lombok** for boilerplate reduction

### Java 21 Features
- **Records** for immutable data
- **Pattern matching** for type safety
- **Virtual threads** for concurrency
- **Text blocks** for readability

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [MongoDB Spring Data](https://spring.io/projects/spring-data-mongodb)
- [RabbitMQ Spring AMQP](https://spring.io/projects/spring-amqp)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

## 🤝 Contributing

1. Follow clean code principles
2. Write comprehensive tests
3. Update documentation
4. Ensure CI/CD pipeline passes

## 📄 License

MIT License - see LICENSE file for details.
