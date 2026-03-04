package com.example.catalog;

import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AuthContainerException extends AuthException {
  protected static final int HTTP_STATUS = 401;

  private final List<AuthException> errors = new ArrayList<>();

  public AuthContainerException() {
    super("", HTTP_STATUS, "", "", Map.of(), null);
  }

  protected AuthContainerException(String errorCode, int httpStatus, String descriptionTemplate,
      String detailTemplate, Map<String, Object> details, Throwable cause) {
    super(errorCode, httpStatus, descriptionTemplate, detailTemplate, details, cause);
  }

  public AuthContainerException add(AuthException error) {
    errors.add(Objects.requireNonNull(error, "error"));
    return this;
  }

  public AuthContainerException addAll(Collection<? extends AuthException> errors) {
    this.errors.addAll(Objects.requireNonNull(errors, "errors"));
    return this;
  }

  public List<AuthException> errors() {
    return List.copyOf(errors);
  }
}
