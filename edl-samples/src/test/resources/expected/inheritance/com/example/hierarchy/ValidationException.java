package com.example.hierarchy;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.Throwable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public abstract class ValidationException extends BaseException {
  private static final String CODE_PREFIX = "VAL";

  protected ValidationException(String errorCode, String descriptionTemplate, String detailTemplate,
      Map<String, Object> details, Throwable cause) {
    super(CODE_PREFIX + Objects.requireNonNull(errorCode, "errorCode"), descriptionTemplate, detailTemplate, details, cause);
  }

  public int httpStatus() {
    return 400;
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
