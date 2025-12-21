package com.example.hierarchy;

import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class MissingFieldException extends InputException {
  public static final String ERROR_CODE = "0099";

  public static final String MESSAGE_TEMPLATE = "Missing {field}";

  public static final boolean RECOVERABLE = false;

  private final String field;

  private MissingFieldException(String field, Map<String, Object> details, Throwable cause) {
    super(ERROR_CODE, MESSAGE_TEMPLATE, Objects.requireNonNull(details, "details"), cause);
    this.field = field;
  }

  public String field() {
    return field;
  }

  public boolean recoverable() {
    return RECOVERABLE;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String field;

    private Throwable cause;

    public Builder field(String field) {
      this.field = field;
      return this;
    }

    public Builder cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    public MissingFieldException build() {
      String resolvedField = this.field;
      if (resolvedField == null) {
        throw new IllegalStateException("Missing required param: " + "field");
      }
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("field", resolvedField);
      return new MissingFieldException(resolvedField, details, cause);
    }
  }
}
