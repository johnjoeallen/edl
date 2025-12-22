package com.example.catalog;

import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class FooErrorException extends AuthException {
  public static final String ERROR_CODE = "0001";

  public static final String DESCRIPTION_TEMPLATE = "Userid invalid {userId}";

  public static final String DETAIL_TEMPLATE = "Userid {userId} does not exist";

  public static final boolean RECOVERABLE = false;

  private final String userId;

  private final String region;

  private FooErrorException(String userId, String region, Map<String, Object> details,
      Throwable cause) {
    super(ERROR_CODE, HTTP_STATUS, DESCRIPTION_TEMPLATE, DETAIL_TEMPLATE, Objects.requireNonNull(details, "details"), cause);
    this.userId = userId;
    this.region = region;
  }

  public String userId() {
    return userId;
  }

  public String region() {
    return region;
  }

  public boolean recoverable() {
    return RECOVERABLE;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String userId;

    private String region;

    private Throwable cause;

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public Builder cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    public FooErrorException build() {
      String resolvedUserId = this.userId;
      if (resolvedUserId == null) {
        throw new IllegalStateException("Missing required param: " + "userId");
      }
      String resolvedRegion = this.region;
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("userId", resolvedUserId);
      details.put("region", resolvedRegion);
      return new FooErrorException(resolvedUserId, resolvedRegion, details, cause);
    }

    public void throwException() {
      throw build();
    }
  }
}
