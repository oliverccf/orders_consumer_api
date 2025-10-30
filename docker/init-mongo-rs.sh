#!/bin/bash

# Initialize MongoDB replica set for development
echo "Initializing MongoDB replica set..."

# Wait for MongoDB to be ready
until mongosh --host mongodb:27017 --eval "print('MongoDB is ready')" > /dev/null 2>&1; do
    echo "Waiting for MongoDB to be ready..."
    sleep 2
done

# Initialize replica set
mongosh --host mongodb:27017 --eval "
rs.initiate({
    _id: 'rs0',
    members: [
        { _id: 0, host: 'mongodb:27017' }
    ]
})
"

echo "MongoDB replica set initialized successfully!"

# Wait for replica set to be ready
echo "Waiting for replica set to be ready..."
mongosh --host mongodb:27017 --eval "
while (rs.status().ok !== 1) {
    print('Waiting for replica set...');
    sleep(1000);
}
print('Replica set is ready!');
"

echo "MongoDB setup completed!"
