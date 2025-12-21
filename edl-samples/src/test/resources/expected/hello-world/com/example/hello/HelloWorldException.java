package com.example.hello;

import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class HelloWorldException extends CommonException {
  public static final String ERROR_CODE = "0001";

  public static final String DESCRIPTION_TEMPLATE = "Hello {name}";

  public static final String DETAIL_TEMPLATE = "Hello detail {name}";

  public static final boolean RECOVERABLE = false;

  private final String name;

  private HelloWorldException(String name, Map<String, Object> details, Throwable cause) {
    super(ERROR_CODE, DESCRIPTION_TEMPLATE, DETAIL_TEMPLATE, Objects.requireNonNull(details, "details"), cause);
    this.name = name;
  }

  public String name() {
    return name;
  }

  public boolean recoverable() {
    return RECOVERABLE;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String name;

    private Throwable cause;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    public HelloWorldException build() {
      String resolvedName = this.name;
      if (resolvedName == null) {
        throw new IllegalStateException("Missing required param: " + "name");
      }
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("name", resolvedName);
      return new HelloWorldException(resolvedName, details, cause);
    }
  }
}
