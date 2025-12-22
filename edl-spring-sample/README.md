# EDL Spring Sample

This module is a small Spring Boot app that uses the EDL Maven plugin to
generate exceptions and a `@RestControllerAdvice` handler.

## Run
```bash
mvn -pl edl-spring-sample spring-boot:run
```

## Try it
GET with missing name (single error):
```bash
curl -s "http://localhost:8080/api/hello"
```

GET with name (single error with templated value):
```bash
curl -s "http://localhost:8080/api/hello?name=Ada"
```

POST (container error list):
```bash
curl -s -X POST "http://localhost:8080/api/hello" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ada"}'
```

Expected shape for container responses:
```json
{
  "Error": [
    {
      "Source": "sample-service",
      "ReasonCode": "CM0002",
      "Description": "Invalid name Ada",
      "Details": "Name Ada is invalid",
      "Recoverable": false
    },
    {
      "Source": "sample-service",
      "ReasonCode": "CM0001",
      "Description": "Missing name",
      "Details": "Name is required",
      "Recoverable": false
    }
  ]
}
```

POST with Errors wrapper:
```bash
curl -s -X POST "http://localhost:8080/api/wrapped" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ada"}'
```

Expected shape for wrapped container responses:
```json
{
  "Errors": [
    {
      "Error": {
        "Source": "sample-service",
        "ReasonCode": "WR0002",
        "Description": "Invalid token Ada",
        "Details": "Token Ada is invalid",
        "Recoverable": false
      }
    },
    {
      "Error": {
        "Source": "sample-service",
        "ReasonCode": "WR0001",
        "Description": "Missing token",
        "Details": "Token is required",
        "Recoverable": false
      }
    }
  ]
}
```

## Insomnia collection
Import `edl-spring-sample/insomnia.json`.
