package com.example.catalog;

public final class AuthContainerException extends CatalogContainerException {
  private static final int HTTP_STATUS = 401;

  public AuthContainerException() {
    super(HTTP_STATUS);
  }

  public AuthContainerException add(AuthException error) {
    super.add(error);
    return this;
  }
}
