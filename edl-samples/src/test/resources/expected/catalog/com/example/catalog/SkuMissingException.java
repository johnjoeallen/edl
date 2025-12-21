package com.example.catalog;

import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SkuMissingException extends NotFoundException {
  public static final String ERROR_CODE = "0003";

  public static final String MESSAGE_TEMPLATE = "SKU {sku} not found";

  public static final boolean RECOVERABLE = false;

  private final String sku;

  private SkuMissingException(String sku, Map<String, Object> details, Throwable cause) {
    super(ERROR_CODE, MESSAGE_TEMPLATE, Objects.requireNonNull(details, "details"), cause);
    this.sku = sku;
  }

  public String sku() {
    return sku;
  }

  public boolean recoverable() {
    return RECOVERABLE;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String sku;

    private Throwable cause;

    public Builder sku(String sku) {
      this.sku = sku;
      return this;
    }

    public Builder cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    public SkuMissingException build() {
      String resolvedSku = this.sku;
      if (resolvedSku == null) {
        throw new IllegalStateException("Missing required param: " + "sku");
      }
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("sku", resolvedSku);
      return new SkuMissingException(resolvedSku, details, cause);
    }
  }
}
