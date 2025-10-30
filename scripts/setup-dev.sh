#!/bin/bash

# Development setup script for Order Service

echo "🚀 Setting up Order Service Development Environment"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Create logs directory
mkdir -p logs

# Start infrastructure services
echo "📦 Starting infrastructure services..."
docker compose -f docker/docker-compose.dev.yml up -d mongodb rabbitmq

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 10

# Initialize MongoDB replica set
echo "🔧 Initializing MongoDB replica set..."
bash docker/init-mongo-rs.sh

# Build and start the application
echo "🏗️ Building and starting the application..."
docker compose -f docker/docker-compose.dev.yml up -d order-service

echo "✅ Development environment is ready!"
echo ""
echo "📋 Service URLs:"
echo "   - Application: http://localhost:8080"
echo "   - API Documentation: http://localhost:8080/swagger-ui.html"
echo "   - RabbitMQ Management: http://localhost:15672 (guest/guest)"
echo "   - MongoDB: mongodb://admin:password@localhost:27017/orders"
echo ""
echo "🔍 To view logs:"
echo "   docker compose -f docker/docker-compose.dev.yml logs -f"
echo ""
echo "🛑 To stop services:"
echo "   docker compose -f docker/docker-compose.dev.yml down"
