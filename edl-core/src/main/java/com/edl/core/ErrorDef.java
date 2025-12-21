package com.edl.core;

import java.util.LinkedHashMap;

public final class ErrorDef {
  private final String name;
  private final String category;
  private final String numericCode;
  private final String description;
  private final String detail;
  private final LinkedHashMap<String, String> requiredParams;
  private final LinkedHashMap<String, String> optionalParams;
  private final boolean recoverable;

  public ErrorDef(String name,
                  String category,
                  String numericCode,
                  String description,
                  String detail,
                  LinkedHashMap<String, String> requiredParams,
                  LinkedHashMap<String, String> optionalParams,
                  boolean recoverable) {
    this.name = name;
    this.category = category;
    this.numericCode = numericCode;
    this.description = description;
    this.detail = detail;
    this.requiredParams = requiredParams;
    this.optionalParams = optionalParams;
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

  public String getDescription() {
    return description;
  }

  public String getDetail() {
    return detail;
  }

  public LinkedHashMap<String, String> getRequiredParams() {
    return requiredParams;
  }

  public LinkedHashMap<String, String> getOptionalParams() {
    return optionalParams;
  }

  public boolean isRecoverable() {
    return recoverable;
  }
}
