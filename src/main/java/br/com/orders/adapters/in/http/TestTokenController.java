package br.com.orders.adapters.in.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@Slf4j
@Tag(name = "Test", description = "Test utilities for development")
public class TestTokenController {
    
    @GetMapping("/token")
    @Operation(summary = "Generate test JWT token", description = "Generate a test JWT token for API testing")
    public ResponseEntity<Map<String, String>> generateTestToken() {
        log.info("Generating test JWT token");
        
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
        
        // Encode header and payload
        String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(header.toString().getBytes());
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.toString().getBytes());
        
        // Create signature (simplified for testing)
        String signature = "test-signature";
        String encodedSignature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signature.getBytes());
        
        // Create JWT token
        String token = encodedHeader + "." + encodedPayload + "." + encodedSignature;
        
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        response.put("expires_in", "3600");
        response.put("scope", "orders:read orders:ack");
        
        log.info("Generated test token successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/orders")
    @Operation(summary = "Test orders endpoint", description = "Test orders endpoint without authentication")
    public ResponseEntity<Map<String, String>> testOrders() {
        log.info("Testing orders endpoint");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Orders endpoint is accessible");
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
