package com.edl.core;

import java.util.LinkedHashMap;
import java.util.List;

public final class ErrorDef {
  private final String name;
  private final String category;
  private final String numericCode;
  private final String message;
  private final LinkedHashMap<String, String> params;
  private final List<String> requiredParams;
  private final boolean recoverable;

  public ErrorDef(String name,
                  String category,
                  String numericCode,
                  String message,
                  LinkedHashMap<String, String> params,
                  List<String> requiredParams,
                  boolean recoverable) {
    this.name = name;
    this.category = category;
    this.numericCode = numericCode;
    this.message = message;
    this.params = params;
    this.requiredParams = requiredParams;
    this.recoverable = recoverable;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  public String getNumericCode() {
    return numericCode;
  }

  public String getMessage() {
    return message;
  }

  public LinkedHashMap<String, String> getParams() {
    return params;
  }

  public List<String> getRequiredParams() {
    return requiredParams;
  }

  public boolean isRecoverable() {
    return recoverable;
  }
}
