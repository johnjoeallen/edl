package com.example.hierarchy;

import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.Map;
import java.util.Objects;

public abstract class InputException extends ValidationException {
  private static final String CODE_PREFIX = "IN";

  protected InputException(String errorCode, String messageTemplate, Map<String, Object> details,
      Throwable cause) {
    super(CODE_PREFIX + Objects.requireNonNull(errorCode, "errorCode"), messageTemplate, details, cause);
  }

  public boolean retryable() {
    return false;
  }
}
