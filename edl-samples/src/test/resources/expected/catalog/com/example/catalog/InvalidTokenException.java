package com.example.catalog;

import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class InvalidTokenException extends AuthException {
  public static final String ERROR_CODE = "0001";

  public static final String MESSAGE_TEMPLATE = "Invalid token {tokenId}";

  public static final boolean RECOVERABLE = false;

  private final String tokenId;

  private InvalidTokenException(String tokenId, Map<String, Object> details, Throwable cause) {
    super(ERROR_CODE, MESSAGE_TEMPLATE, Objects.requireNonNull(details, "details"), cause);
    this.tokenId = tokenId;
  }

  public String tokenId() {
    return tokenId;
  }

  public boolean recoverable() {
    return RECOVERABLE;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String tokenId;

    private Throwable cause;

    public Builder tokenId(String tokenId) {
      this.tokenId = tokenId;
      return this;
    }

    public Builder cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    public InvalidTokenException build() {
      String resolvedTokenId = this.tokenId;
      if (resolvedTokenId == null) {
        throw new IllegalStateException("Missing required param: " + "tokenId");
      }
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("tokenId", resolvedTokenId);
      return new InvalidTokenException(resolvedTokenId, details, cause);
    }
  }
}
