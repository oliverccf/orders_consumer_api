package br.com.orders.adapters.in.messaging.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
public class JsonSchemaValidator {
    
    private final JsonSchema orderCreatedSchema;
    private final ObjectMapper objectMapper;
    
    public JsonSchemaValidator(ObjectMapper objectMapper) throws IOException, ProcessingException {
        this.objectMapper = objectMapper;
        this.orderCreatedSchema = loadSchema("schemas/OrderCreated.schema.json");
    }
    
    public void validateOrderCreated(String jsonMessage) {
        try {
            log.debug("Validating JSON message: {}", jsonMessage);
            
            // First, parse the JSON string to ensure it's valid JSON
            JsonNode jsonNode = objectMapper.readTree(jsonMessage);
            log.debug("Parsed JSON node type: {}, value: {}", jsonNode.getNodeType(), jsonNode);
            
            // Check if the parsed node is actually an object
            if (!jsonNode.isObject()) {
                log.error("Parsed JSON is not an object, type: {}", jsonNode.getNodeType());
                throw new JsonSchemaValidationException("JSON message must be an object, but got: " + jsonNode.getNodeType());
            }
            
            // Manual validation of required fields
            validateRequiredFields(jsonNode);
            
            log.debug("JSON Schema validation passed");
            
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Invalid JSON format: {}", e.getMessage(), e);
            throw new JsonSchemaValidationException("Invalid JSON format", e);
        } catch (Exception e) {
            log.error("Error validating JSON schema: {}", e.getMessage(), e);
            throw new JsonSchemaValidationException("Failed to validate message", e);
        }
    }
    
    private void validateRequiredFields(JsonNode jsonNode) {
        // Check required fields
        if (!jsonNode.has("externalId") || jsonNode.get("externalId").isNull()) {
            throw new JsonSchemaValidationException("Missing required field: externalId");
        }
        
        if (!jsonNode.has("items") || !jsonNode.get("items").isArray()) {
            throw new JsonSchemaValidationException("Missing required field: items (must be an array)");
        }
        
        JsonNode items = jsonNode.get("items");
        if (items.size() == 0) {
            throw new JsonSchemaValidationException("Items array cannot be empty");
        }
        
        // Validate each item
        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            if (!item.isObject()) {
                throw new JsonSchemaValidationException("Item at index " + i + " must be an object");
            }
            
            if (!item.has("productId") || item.get("productId").isNull()) {
                throw new JsonSchemaValidationException("Missing required field: productId in item " + i);
            }
            
            if (!item.has("productName") || item.get("productName").isNull()) {
                throw new JsonSchemaValidationException("Missing required field: productName in item " + i);
            }
            
            if (!item.has("unitPrice") || !item.get("unitPrice").isNumber()) {
                throw new JsonSchemaValidationException("Missing or invalid field: unitPrice in item " + i + " (must be a number)");
            }
            
            if (!item.has("quantity") || !item.get("quantity").isNumber()) {
                throw new JsonSchemaValidationException("Missing or invalid field: quantity in item " + i + " (must be a number)");
            }
            
            // Validate unitPrice is positive
            double unitPrice = item.get("unitPrice").asDouble();
            if (unitPrice < 0) {
                throw new JsonSchemaValidationException("unitPrice in item " + i + " must be positive");
            }
            
            // Validate quantity is positive integer
            int quantity = item.get("quantity").asInt();
            if (quantity <= 0) {
                throw new JsonSchemaValidationException("quantity in item " + i + " must be a positive integer");
            }
        }
        
        // Validate correlationId if present
        if (jsonNode.has("correlationId") && jsonNode.get("correlationId").isNull()) {
            throw new JsonSchemaValidationException("correlationId cannot be null if present");
        }
    }
    
    private JsonSchema loadSchema(String schemaPath) throws IOException, ProcessingException {
        ClassPathResource resource = new ClassPathResource(schemaPath);
        try (InputStream inputStream = resource.getInputStream()) {
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            return factory.getJsonSchema(objectMapper.readTree(inputStream));
        }
    }
    
    public static class JsonSchemaValidationException extends RuntimeException {
        public JsonSchemaValidationException(String message) {
            super(message);
        }
        
        public JsonSchemaValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
