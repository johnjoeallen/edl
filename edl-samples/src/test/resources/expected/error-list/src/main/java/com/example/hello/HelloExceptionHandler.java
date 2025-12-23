package com.example.hello;

import java.lang.Object;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class HelloExceptionHandler extends ExceptionHandlerBase {
  @ExceptionHandler(HelloException.class)
  public ResponseEntity<Map<String, Object>> handleHelloException(HelloException exception) {
    Map<String, Object> body = mapResponse(exception.errorInfo());
    return ResponseEntity.status(exception.httpStatus()).body(body);
  }

  @ExceptionHandler(CommonContainerException.class)
  public ResponseEntity<Map<String, Object>> handleCommonContainerException(CommonContainerException exception) {
    List<Map<String, Object>> infos = new ArrayList<>();
    for (HelloException error : exception.errors()) {
      infos.add(error.errorInfo());
    }
    Object rendered = renderContainerTemplate(CONTAINER_TEMPLATE, infos);
    return ResponseEntity.status(exception.httpStatus()).body((Map<String, Object>) rendered);
  }
}
