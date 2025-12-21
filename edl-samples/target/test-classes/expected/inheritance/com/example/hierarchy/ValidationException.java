package com.example.hierarchy;

import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.Map;
import java.util.Objects;

public abstract class ValidationException extends BaseException {
  private static final String CODE_PREFIX = "VAL";

  protected ValidationException(String errorCode, String messageTemplate,
      Map<String, Object> details, Throwable cause) {
    super(CODE_PREFIX + Objects.requireNonNull(errorCode, "errorCode"), messageTemplate, details, cause);
  }

  public int httpStatus() {
    return 400;
  }
}
