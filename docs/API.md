# Order Service API Documentation

## Overview
This document describes the REST API for the Order Service, a microservice that manages order processing and provides endpoints for external systems to consume orders.

## Base URL
```
http://localhost:8080/api/v1
```

## Authentication
All API endpoints require JWT authentication. Include the JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

## Scopes
The API uses OAuth2 scopes for authorization:
- `orders:read` - Required for reading orders
- `orders:ack` - Required for acknowledging orders

## Endpoints

### List Orders
Retrieve orders filtered by status.

**GET** `/orders`

#### Parameters
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| status | string | No | AVAILABLE_FOR_B | Order status filter |
| page | integer | No | 0 | Page number (0-based) |
| size | integer | No | 20 | Page size |
| sort | string | No | updatedAt,desc | Sort criteria |

#### Response
```json
{
  "content": [
    {
      "id": "ORDER-001",
      "externalId": "EXT-001",
      "status": "AVAILABLE_FOR_B",
      "items": [
        {
          "productId": "PROD-001",
          "productName": "Product 1",
          "unitPrice": 10.50,
          "quantity": 2,
          "totalPrice": 21.00
        }
      ],
      "totalAmount": 21.00,
      "createdAt": "2024-01-01T10:00:00Z",
      "updatedAt": "2024-01-01T10:00:00Z",
      "correlationId": "CORR-001",
      "version": 1
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "numberOfElements": 1
}
```

#### Example
```bash
curl -H "Authorization: Bearer <token>" \
     "http://localhost:8080/api/v1/orders?status=AVAILABLE_FOR_B&page=0&size=10"
```

### Get Order by ID
Retrieve a specific order by its ID.

**GET** `/orders/{id}`

#### Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | string | Yes | Order ID |

#### Response
```json
{
  "id": "ORDER-001",
  "externalId": "EXT-001",
  "status": "AVAILABLE_FOR_B",
  "items": [
    {
      "productId": "PROD-001",
      "productName": "Product 1",
      "unitPrice": 10.50,
      "quantity": 2,
      "totalPrice": 21.00
    }
  ],
  "totalAmount": 21.00,
  "createdAt": "2024-01-01T10:00:00Z",
  "updatedAt": "2024-01-01T10:00:00Z",
  "correlationId": "CORR-001",
  "version": 1
}
```

#### Example
```bash
curl -H "Authorization: Bearer <token>" \
     "http://localhost:8080/api/v1/orders/ORDER-001"
```

### Acknowledge Order
Confirm receipt of an order (optimistic locking with version control).

**POST** `/orders/{id}/ack`

#### Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | string | Yes | Order ID |

#### Headers
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| If-Match | string | Yes | Expected version for optimistic locking |

#### Response
```json
{
  "id": "ORDER-001",
  "externalId": "EXT-001",
  "status": "ACKNOWLEDGED",
  "items": [
    {
      "productId": "PROD-001",
      "productName": "Product 1",
      "unitPrice": 10.50,
      "quantity": 2,
      "totalPrice": 21.00
    }
  ],
  "totalAmount": 21.00,
  "createdAt": "2024-01-01T10:00:00Z",
  "updatedAt": "2024-01-01T10:05:00Z",
  "correlationId": "CORR-001",
  "version": 2
}
```

#### Example
```bash
curl -X POST \
     -H "Authorization: Bearer <token>" \
     -H "If-Match: 1" \
     "http://localhost:8080/api/v1/orders/ORDER-001/ack"
```

## Status Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 409 | Conflict (Version mismatch) |
| 500 | Internal Server Error |

## Error Responses

### Validation Error
```json
{
  "timestamp": "2024-01-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/orders"
}
```

### Authentication Error
```json
{
  "timestamp": "2024-01-01T10:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token is missing or invalid",
  "path": "/api/v1/orders"
}
```

### Authorization Error
```json
{
  "timestamp": "2024-01-01T10:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient scope: orders:read required",
  "path": "/api/v1/orders"
}
```

### Version Conflict
```json
{
  "timestamp": "2024-01-01T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Version mismatch. Expected: 1, Actual: 2",
  "path": "/api/v1/orders/ORDER-001/ack"
}
```

## Order Statuses

| Status | Description |
|--------|-------------|
| PROCESSING | Order is being processed |
| AVAILABLE_FOR_B | Order is ready for external system consumption |
| ACKNOWLEDGED | Order has been confirmed by external system |
| FAILED | Order processing failed |

## Rate Limiting
Currently no rate limiting is implemented. Consider implementing rate limiting for production environments.

## Pagination
All list endpoints support pagination with the following parameters:
- `page`: Page number (0-based)
- `size`: Number of items per page
- `sort`: Sort criteria (e.g., `updatedAt,desc`)

## Filtering
Orders can be filtered by status using the `status` query parameter.

## Sorting
Default sorting is by `updatedAt` in descending order. Custom sorting can be specified using the `sort` parameter.

## CORS
CORS is enabled for all origins in development. Configure appropriate CORS policies for production.

## Monitoring
The API includes health check endpoints:
- `/actuator/health` - Application health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics format
