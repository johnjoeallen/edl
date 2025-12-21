package com.example.catalog;

import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.Map;
import java.util.Objects;

public abstract class BillingException extends CatalogException {
  private static final String CODE_PREFIX = "BILL";

  protected BillingException(String errorCode, String messageTemplate, Map<String, Object> details,
      Throwable cause) {
    super(CODE_PREFIX + Objects.requireNonNull(errorCode, "errorCode"), messageTemplate, details, cause);
  }

  public boolean retryable() {
    return true;
  }
}
