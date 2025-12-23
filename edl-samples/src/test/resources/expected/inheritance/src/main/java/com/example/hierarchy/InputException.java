package com.example.hierarchy;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.Throwable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public abstract class InputException extends ValidationException {
  private static final String CODE_PREFIX = "IN";

  protected static final int HTTP_STATUS = 400;

  protected InputException(String errorCode, int httpStatus, String descriptionTemplate,
      String detailTemplate, Map<String, Object> details, Throwable cause) {
    super(CODE_PREFIX + Objects.requireNonNull(errorCode, "errorCode"), httpStatus, descriptionTemplate, detailTemplate, details, cause);
  }

  public boolean retryable() {
    return false;
  }

  @Override
  protected Map<String, Object> coreValues() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("source", source());
    values.put("code", code());
    values.put("description", description());
    values.put("details", detail());
    return Map.copyOf(values);
  }
}
