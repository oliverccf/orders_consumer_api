Feature: Order Processing
  As a system
  I want to process orders from external systems
  So that orders can be calculated and made available for consumption

  Scenario: Process a new order successfully
    Given an order with external ID "EXT-001" and correlation ID "CORR-001"
    When the order message is sent to the incoming queue
    Then the order should be processed and saved in the database
    And the order should have the correct total amount calculated

  Scenario: Handle duplicate order messages with idempotency
    Given an existing order with external ID "EXT-002"
    When a duplicate order message is sent
    Then the order should be updated with idempotency

  Scenario: Process order with multiple items
    Given an order with external ID "EXT-003" and correlation ID "CORR-003"
    When the order message is sent to the incoming queue
    Then the order should be processed and saved in the database
    And the order should have the correct total amount calculated

  Scenario: End-to-end order processing with API validation
    Given an order with external ID "EXT-004" and correlation ID "CORR-004"
    When the order message is sent to the incoming queue
    And I wait for the order to be processed
    And I generate an authentication token
    When I query the order via API using the token
    Then the order should be retrieved successfully from the API
    And the API response should match the order in the database
