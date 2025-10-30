package br.com.orders.component;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to generate test JWT tokens for component tests
 */
public class TestTokenHelper {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Generates a test JWT token with the required scopes for order service
     * @return JWT token string
     */
    public static String generateTestToken() {
        try {
            // Create JWT header
            Map<String, String> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            
            // Create JWT payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", "test-user");
            payload.put("iss", "http://localhost:8080/auth/realms/orders");
            payload.put("aud", "order-service");
            payload.put("exp", Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond());
            payload.put("iat", Instant.now().getEpochSecond());
            payload.put("scope", "orders:read orders:ack");
            payload.put("authorities", new String[]{"SCOPE_orders:read", "SCOPE_orders:ack"});
            
            // Encode header and payload as JSON strings first, then base64
            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes());
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes());
            
            // Create signature (simplified for testing - in real scenario this would be properly signed)
            String signature = "test-signature";
            String encodedSignature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(signature.getBytes());
            
            // Create JWT token
            return encodedHeader + "." + encodedPayload + "." + encodedSignature;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test token", e);
        }
    }
    
    /**
     * Gets the Bearer token format for Authorization header
     * @return "Bearer {token}"
     */
    public static String getBearerToken() {
        return "Bearer " + generateTestToken();
    }
}

