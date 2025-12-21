package com.example.hello;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.Object;
import java.lang.String;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class HelloRootExceptionHandler {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @ExceptionHandler(HelloRootException.class)
  public ResponseEntity<Map<String, Object>> handleHelloRootException(
      HelloRootException exception) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("source", exception.source());
    body.put("reasonCode", exception.code());
    body.put("description", exception.errorInfo().get("description"));
    body.put("recoverable", exception.recoverable());
    body.put("detailsJson", toJson(exception.details()));
    return ResponseEntity.status(500).body(body);
  }

  private static String toJson(Map<String, Object> details) {
    try {
      return MAPPER.writeValueAsString(details);
    } catch (Exception ex) {
      return "{}";
    }
  }
}
