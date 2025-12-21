package com.example.hello;

import java.lang.Object;
import java.lang.String;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class HelloExceptionHandler {
  @ExceptionHandler(HelloException.class)
  public ResponseEntity<Map<String, Object>> handleHelloException(
      HelloException exception) {
    Map<String, Object> info = exception.errorInfo();
    Map<String, Object> body = new LinkedHashMap<>();
    if (info.containsKey("source")) {
      body.put("Source", info.get("source"));
    }
    if (info.containsKey("code")) {
      body.put("ReasonCode", info.get("code"));
    }
    if (info.containsKey("description")) {
      body.put("Description", info.get("description"));
    }
    if (info.containsKey("details")) {
      body.put("Details", info.get("details"));
    }
    if (info.containsKey("recoverable")) {
      body.put("Recoverable", info.get("recoverable"));
    }
    return ResponseEntity.status(exception.httpStatus()).body(body);
  }

}
