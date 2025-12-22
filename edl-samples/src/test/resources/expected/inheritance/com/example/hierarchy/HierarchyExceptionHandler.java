package com.example.hierarchy;

import java.lang.Object;
import java.lang.String;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class HierarchyExceptionHandler extends ExceptionHandlerBase {

  @ExceptionHandler(HierarchyException.class)
  public ResponseEntity<Map<String, Object>> handleHierarchyException(
      HierarchyException exception) {
    Map<String, Object> body = mapResponse(exception.errorInfo());
    return ResponseEntity.status(exception.httpStatus()).body(body);
  }
}
