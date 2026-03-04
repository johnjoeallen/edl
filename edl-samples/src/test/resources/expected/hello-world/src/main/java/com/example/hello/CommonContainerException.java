package com.example.hello;

import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CommonContainerException extends CommonException {
  protected static final int HTTP_STATUS = 500;

  private final List<CommonException> errors = new ArrayList<>();

  public CommonContainerException() {
    super("", HTTP_STATUS, "", "", Map.of(), null);
  }

  protected CommonContainerException(String errorCode, int httpStatus, String descriptionTemplate,
      String detailTemplate, Map<String, Object> details, Throwable cause) {
    super(errorCode, httpStatus, descriptionTemplate, detailTemplate, details, cause);
  }

  public CommonContainerException add(CommonException error) {
    errors.add(Objects.requireNonNull(error, "error"));
    return this;
  }

  public CommonContainerException addAll(Collection<? extends CommonException> errors) {
    this.errors.addAll(Objects.requireNonNull(errors, "errors"));
    return this;
  }

  public List<CommonException> errors() {
    return List.copyOf(errors);
  }
}
