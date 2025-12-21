package com.example.catalog;

import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RetryLaterException extends BillingException {
  public static final String ERROR_CODE = "0006";

  public static final String MESSAGE_TEMPLATE = "Retry later after {seconds}";

  public static final boolean RECOVERABLE = false;

  private final int seconds;

  private RetryLaterException(int seconds, Map<String, Object> details, Throwable cause) {
    super(ERROR_CODE, MESSAGE_TEMPLATE, Objects.requireNonNull(details, "details"), cause);
    this.seconds = seconds;
  }

  public int seconds() {
    return seconds;
  }

  public boolean recoverable() {
    return RECOVERABLE;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Integer seconds;

    private Throwable cause;

    public Builder seconds(int seconds) {
      this.seconds = seconds;
      return this;
    }

    public Builder cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    public RetryLaterException build() {
      Integer resolvedSeconds = this.seconds;
      if (resolvedSeconds == null) {
        throw new IllegalStateException("Missing required param: " + "seconds");
      }
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("seconds", resolvedSeconds);
      return new RetryLaterException(resolvedSeconds.intValue(), details, cause);
    }
  }
}
