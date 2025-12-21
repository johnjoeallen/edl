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

  protected AuthException(String errorCode, String descriptionTemplate, String detailTemplate,
      Map<String, Object> details, Throwable cause) {
    super(CODE_PREFIX + Objects.requireNonNull(errorCode, "errorCode"), descriptionTemplate, detailTemplate, details, cause);
  }

  public int httpStatus() {
    return 401;
  }

  @Override
  protected Map<String, Object> coreValues() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("source", source());
    values.put("code", code());
    values.put("description", description());
    values.put("detail", detail());
    return Map.copyOf(values);
  }
}
