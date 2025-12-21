package com.example.catalog;

import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class InvalidPriceException extends ValidationException {
  public static final String ERROR_CODE = "0004";

  public static final String MESSAGE_TEMPLATE = "Invalid price {price}";

  public static final boolean RECOVERABLE = false;

  private final BigDecimal price;

  private InvalidPriceException(BigDecimal price, Map<String, Object> details, Throwable cause) {
    super(ERROR_CODE, MESSAGE_TEMPLATE, Objects.requireNonNull(details, "details"), cause);
    this.price = price;
  }

  public BigDecimal price() {
    return price;
  }

  public boolean recoverable() {
    return RECOVERABLE;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private BigDecimal price;

    private Throwable cause;

    public Builder price(BigDecimal price) {
      this.price = price;
      return this;
    }

    public Builder cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    public InvalidPriceException build() {
      BigDecimal resolvedPrice = this.price;
      if (resolvedPrice == null) {
        throw new IllegalStateException("Missing required param: " + "price");
      }
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("price", resolvedPrice);
      return new InvalidPriceException(resolvedPrice, details, cause);
    }
  }
}
