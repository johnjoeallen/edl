package com.example.hierarchy;

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
public class HierarchyExceptionHandler {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @ExceptionHandler(HierarchyException.class)
  public ResponseEntity<Map<String, Object>> handleHierarchyException(
      HierarchyException exception) {
    Map<String, Object> info = exception.errorInfo();
    Map<String, Object> body = new LinkedHashMap<>();
    if (info.containsKey("source")) {
      body.put("source", info.get("source"));
    }
    if (info.containsKey("code")) {
      body.put("code", info.get("code"));
    }
    if (info.containsKey("description")) {
      body.put("description", info.get("description"));
    }
    if (info.containsKey("detail")) {
      body.put("detail", info.get("detail"));
    }
    if (info.containsKey("details")) {
      body.put("details", toJson((Map<String, Object>) info.get("details")));
    }
    if (info.containsKey("recoverable")) {
      body.put("recoverable", info.get("recoverable"));
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
