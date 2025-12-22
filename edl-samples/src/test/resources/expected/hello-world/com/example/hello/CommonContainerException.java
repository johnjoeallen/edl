package com.example.hello;

import java.lang.Override;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CommonContainerException extends HelloException {
  private static final int HTTP_STATUS = 500;

  private final List<CommonException> errors = new ArrayList<>();

  public CommonContainerException() {
    super("", HTTP_STATUS, "", "", Map.of(), null);
  }

  public void add(CommonException error) {
    errors.add(Objects.requireNonNull(error, "error"));
  }

  public void addAll(Collection<? extends CommonException> errors) {
    this.errors.addAll(Objects.requireNonNull(errors, "errors"));
  }

  public List<CommonException> errors() {
    return List.copyOf(errors);
  }

  public boolean recoverable() {
    return false;
  }

  @Override
  protected Map<String, Object> coreValues() {
    return Map.of();
  }
}
