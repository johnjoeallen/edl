package com.example.catalog;

import java.lang.RuntimeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public abstract class ContainerExceptionBase extends RuntimeException {
  private final int httpStatus;

  protected final List<CatalogException> errors = new ArrayList<>();

  protected ContainerExceptionBase(int httpStatus) {
    super();
    this.httpStatus = httpStatus;
  }

  public void add(CatalogException error) {
    errors.add(Objects.requireNonNull(error, "error"));
  }

  public void addAll(Collection<? extends CatalogException> errors) {
    this.errors.addAll(Objects.requireNonNull(errors, "errors"));
  }

  public List<CatalogException> errors() {
    return List.copyOf(errors);
  }

  public int httpStatus() {
    return httpStatus;
  }
}
