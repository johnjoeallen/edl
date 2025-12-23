package com.example.hello;

import java.lang.RuntimeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public abstract class ContainerExceptionBase extends RuntimeException {
  private final int httpStatus;

  protected final List<HelloException> errors = new ArrayList<>();

  protected ContainerExceptionBase(int httpStatus) {
    super();
    this.httpStatus = httpStatus;
  }

  public void add(HelloException error) {
    errors.add(Objects.requireNonNull(error, "error"));
  }

  public void addAll(Collection<? extends HelloException> errors) {
    this.errors.addAll(Objects.requireNonNull(errors, "errors"));
  }

  public List<HelloException> errors() {
    return List.copyOf(errors);
  }

  public int httpStatus() {
    return httpStatus;
  }
}
