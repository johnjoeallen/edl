package com.example.catalog;

import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class UserMissingException extends NotFoundException {
  public static final String ERROR_CODE = "0002";

  public static final String MESSAGE_TEMPLATE = "User {userId} not found";

  public static final boolean RECOVERABLE = false;

  private final String userId;

  private UserMissingException(String userId, Map<String, Object> details, Throwable cause) {
    super(ERROR_CODE, MESSAGE_TEMPLATE, Objects.requireNonNull(details, "details"), cause);
    this.userId = userId;
  }

  public String userId() {
    return userId;
  }

  public boolean recoverable() {
    return RECOVERABLE;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String userId;

    private Throwable cause;

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    public UserMissingException build() {
      String resolvedUserId = this.userId;
      if (resolvedUserId == null) {
        throw new IllegalStateException("Missing required param: " + "userId");
      }
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("userId", resolvedUserId);
      return new UserMissingException(resolvedUserId, details, cause);
    }
  }
}
