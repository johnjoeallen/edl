package com.example.catalog;

import java.lang.Object;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CatalogExceptionHandler extends ExceptionHandlerBase {
  @ExceptionHandler(CatalogException.class)
  public ResponseEntity<Map<String, Object>> handleCatalogException(CatalogException exception) {
    Map<String, Object> body = mapResponse(exception.errorInfo());
    return ResponseEntity.status(exception.httpStatus()).body(body);
  }

  @ExceptionHandler(AuthContainerException.class)
  public ResponseEntity<Map<String, Object>> handleAuthContainerException(AuthContainerException exception) {
    List<Map<String, Object>> infos = new ArrayList<>();
    for (CatalogException error : exception.errors()) {
      infos.add(error.errorInfo());
    }
    Object rendered = renderContainerTemplate(CONTAINER_TEMPLATE, infos);
    return ResponseEntity.status(exception.httpStatus()).body((Map<String, Object>) rendered);
  }
}
