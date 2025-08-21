# JohnDeere Challenge

This project is a Spring Boot application that integrates with Kafka and MongoDB, both of which are provided via the included Docker Compose file.

## Requirements
- Docker & Docker Compose
- Java 17+
- Maven

## Setup
1. **Start Kafka and MongoDB**
   
   Use the provided `docker-compose.yaml` to start the required services:
   
   ```bash
   docker compose up -d
   ```

2. **Configuration**
   
   The connection settings for Kafka and MongoDB are managed via Spring configuration in `src/main/resources/application.yml`. You can adjust the connection details there if needed.

## Application Behavior
- On startup, the application automatically sends 2 messages to Kafka, as specified in the challenge documentation.
- The service then makes an API call to the PetClient, which simulates a call to a third-party service. This client was generated using the Maven OpenAPI Generator plugin.
- After processing, the service checks the message against a waitlist (provided via environment variable). If the message matches the criteria, it is sent to another Kafka topic.

  ## Test the endpoint with curl

You can test the message sending endpoint with the following command:

```bash
curl --location 'http://localhost:8080/api/messages?topic=inbound_message_queue' \
--header 'Content-Type: application/json' \
--data '{
    "sessionGuid": "a65de8c4-6385-4008-be36-5df0c5104fd5",
    "sequenceNumber": 1,
    "machineId": 1,
    "data": [
      {
        "type": "DISTANCE",
        "unit": "m",
        "value": "100"
      },
      {
        "type": "WORKED_SURFACE",
        "unit": "m2",
        "value": "600"
      }
    ]
  }'
```

You can use uppercase, lowercase, or camelCase for the `type` field (e.g., `distance`, `workedSurface`, etc.).