package com.example.catalog;

import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PaymentDeclinedException extends BillingException {
  public static final String ERROR_CODE = "0005";

  public static final String MESSAGE_TEMPLATE = "Payment declined {paymentId}";

  public static final boolean RECOVERABLE = false;

  private final String paymentId;

  private PaymentDeclinedException(String paymentId, Map<String, Object> details, Throwable cause) {
    super(ERROR_CODE, MESSAGE_TEMPLATE, Objects.requireNonNull(details, "details"), cause);
    this.paymentId = paymentId;
  }

  public String paymentId() {
    return paymentId;
  }

  public boolean recoverable() {
    return RECOVERABLE;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String paymentId;

    private Throwable cause;

    public Builder paymentId(String paymentId) {
      this.paymentId = paymentId;
      return this;
    }

    public Builder cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    public PaymentDeclinedException build() {
      String resolvedPaymentId = this.paymentId;
      if (resolvedPaymentId == null) {
        throw new IllegalStateException("Missing required param: " + "paymentId");
      }
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("paymentId", resolvedPaymentId);
      return new PaymentDeclinedException(resolvedPaymentId, details, cause);
    }
  }
}
