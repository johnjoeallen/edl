package com.example.hierarchy;

import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class InvalidEmailException extends ValidationException {
  public static final String ERROR_CODE = "0012";

  public static final String DESCRIPTION_TEMPLATE = "Invalid email {email}";

  public static final String DETAIL_TEMPLATE = "Invalid email {email} detail";

  public static final boolean RECOVERABLE = false;

  private final String email;

  private InvalidEmailException(String email, Map<String, Object> details, Throwable cause) {
    super(ERROR_CODE, HTTP_STATUS, DESCRIPTION_TEMPLATE, DETAIL_TEMPLATE, Objects.requireNonNull(details, "details"), cause);
    this.email = email;
  }

  public String email() {
    return email;
  }

  public boolean recoverable() {
    return RECOVERABLE;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String email;

    private Throwable cause;

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    public InvalidEmailException build() {
      String resolvedEmail = this.email;
      if (resolvedEmail == null) {
        throw new IllegalStateException("Missing required param: " + "email");
      }
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("email", resolvedEmail);
      return new InvalidEmailException(resolvedEmail, details, cause);
    }
  }
}
