package com.example.hello;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.Exception;
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
    Map<String, Object> info = exception.errorInfo();
    Map<String, Object> body = new LinkedHashMap<>();
    if (info.containsKey("source")) {
      body.put("source", info.get("source"));
    }
    if (info.containsKey("code")) {
      body.put("reasonCode", info.get("code"));
    }
    if (info.containsKey("description")) {
      body.put("message", info.get("description"));
    }
    if (info.containsKey("detail")) {
      body.put("detail", info.get("detail"));
    }
    if (info.containsKey("recoverable")) {
      body.put("canRecover", info.get("recoverable"));
    }
    if (info.containsKey("details")) {
      body.put("detailsJson", toJson((Map<String, Object>) info.get("details")));
    }
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
