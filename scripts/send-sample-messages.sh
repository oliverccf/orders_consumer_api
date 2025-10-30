#!/bin/bash

# Sample message producer for testing Order Service

echo "🚀 Order Service Message Producer"
echo "=================================="

# Configuration
RABBITMQ_HOST=${RABBITMQ_HOST:-localhost}
RABBITMQ_PORT=${RABBITMQ_PORT:-5672}
RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-guest}
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-guest}
EXCHANGE_NAME="orders.incoming.ex"
ROUTING_KEY="order.created"

# Function to send a message
send_message() {
    local external_id=$1
    local correlation_id=$2
    local product_id=$3
    local product_name=$4
    local unit_price=$5
    local quantity=$6
    
    local message=$(cat <<EOF
{
    "externalId": "$external_id",
    "correlationId": "$correlation_id",
    "items": [
        {
            "productId": "$product_id",
            "productName": "$product_name",
            "unitPrice": $unit_price,
            "quantity": $quantity
        }
    ]
}
EOF
)
    
    echo "📤 Sending message for order: $external_id"
    echo "Message: $message"
    echo ""
    
    # Use rabbitmqadmin if available, otherwise use curl
    if command -v rabbitmqadmin &> /dev/null; then
        rabbitmqadmin publish exchange="$EXCHANGE_NAME" routing_key="$ROUTING_KEY" payload="$message"
    else
        echo "⚠️  rabbitmqadmin not found. Please install it or use RabbitMQ Management UI to send messages."
        echo "Exchange: $EXCHANGE_NAME"
        echo "Routing Key: $ROUTING_KEY"
        echo "Message Body:"
        echo "$message"
    fi
}

# Send sample messages
echo "📋 Sending sample order messages..."
echo ""

# Order 1: Single item
send_message "EXT-001" "CORR-001" "PROD-001" "Test Product 1" 10.50 2

# Order 2: Multiple items
send_message "EXT-002" "CORR-002" "PROD-002" "Test Product 2" 25.00 1

# Order 3: Large quantity
send_message "EXT-003" "CORR-003" "PROD-003" "Test Product 3" 5.75 10

# Order 4: Duplicate (for idempotency testing)
send_message "EXT-001" "CORR-004" "PROD-001" "Test Product 1" 10.50 2

echo "✅ Sample messages sent!"
echo ""
echo "🔍 Check the application logs to see order processing:"
echo "   docker compose -f docker/docker-compose.dev.yml logs -f order-service"
echo ""
echo "📊 Check orders in MongoDB:"
echo "   mongosh mongodb://admin:password@localhost:27017/orders --authenticationDatabase admin"
echo "   db.orders.find().pretty()"
echo ""
echo "🌐 Check API endpoints:"
echo "   curl http://localhost:8080/api/v1/orders?status=AVAILABLE_FOR_B"
