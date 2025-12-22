package com.example.edlsample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HelloControllerTest {
  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void getHelloReturnsSingleError() throws Exception {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/hello?name=Ada", String.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
    assertEquals("sample-service", body.get("Source"));
    assertEquals("CM0002", body.get("ReasonCode"));
    assertEquals("Invalid name Ada", body.get("Description"));
    assertEquals("Name Ada is invalid", body.get("Details"));
    assertEquals(false, body.get("Recoverable"));
  }

  @Test
  void postHelloReturnsContainerErrors() throws Exception {
    RequestEntity<HelloRequest> request = RequestEntity.post("/api/hello")
        .contentType(MediaType.APPLICATION_JSON)
        .body(new HelloRequest("Ada"));

    ResponseEntity<String> response = restTemplate.exchange(request, String.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
    Object errorNode = body.get("Error");
    assertNotNull(errorNode);
    List<Map<String, Object>> errors = (List<Map<String, Object>>) errorNode;
    assertEquals(2, errors.size());

    Map<String, Object> first = errors.get(0);
    assertEquals("CM0002", first.get("ReasonCode"));
    assertEquals("Invalid name Ada", first.get("Description"));

    Map<String, Object> second = errors.get(1);
    assertEquals("CM0001", second.get("ReasonCode"));
    assertEquals("Missing name", second.get("Description"));
  }

  @Test
  void postWrappedReturnsErrorsWrapper() throws Exception {
    RequestEntity<HelloRequest> request = RequestEntity.post("/api/wrapped")
        .contentType(MediaType.APPLICATION_JSON)
        .body(new HelloRequest("Ada"));

    ResponseEntity<String> response = restTemplate.exchange(request, String.class);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
    Object errorsNode = body.get("Errors");
    assertNotNull(errorsNode);
    List<Map<String, Object>> errors = (List<Map<String, Object>>) errorsNode;
    assertEquals(2, errors.size());

    Map<String, Object> first = (Map<String, Object>) errors.get(0).get("Error");
    assertEquals("WR0002", first.get("ReasonCode"));
    assertEquals("Invalid token Ada", first.get("Description"));

    Map<String, Object> second = (Map<String, Object>) errors.get(1).get("Error");
    assertEquals("WR0001", second.get("ReasonCode"));
    assertEquals("Missing token", second.get("Description"));
  }
}
