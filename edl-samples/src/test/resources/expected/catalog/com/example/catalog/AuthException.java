package com.example.catalog;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.Throwable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AuthException extends CatalogException {
  private static final String CODE_PREFIX = "AUTH";

  protected static final int HTTP_STATUS = 401;

  protected AuthException(String errorCode, int httpStatus, String descriptionTemplate,
      String detailTemplate, Map<String, Object> details, Throwable cause) {
    super(CODE_PREFIX + Objects.requireNonNull(errorCode, "errorCode"), httpStatus, descriptionTemplate, detailTemplate, details, cause);
  }

  @Override
  protected Map<String, Object> coreValues() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("source", source());
    values.put("code", code());
    values.put("description", description());
    values.put("details", details());
    return Map.copyOf(values);
  }
}
