package com.example.hello;

public final class CommonContainerException extends ContainerExceptionBase {
  private static final int HTTP_STATUS = 500;

  public CommonContainerException() {
    super(HTTP_STATUS);
  }
}
