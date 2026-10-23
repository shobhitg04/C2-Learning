# Spec Delta

## Purpose

Sets the backend HTTP listen port so clients reach the ticket API on port 85.

## ADDED Requirements

### Requirement: HTTP listen port
The backend HTTP server MUST be configured to listen on port 85.

#### Scenario: Configured port
- **WHEN** the backend application configuration is loaded
- **THEN** the HTTP listen port is 85
