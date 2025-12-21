package com.edl.core;

public final class CategoryDef {
  private final String name;
  private final String parent;
  private final String codePrefix;
  private final Integer httpStatus;
  private final Boolean retryable;
  private final boolean isAbstract;

  public CategoryDef(String name,
                     String parent,
                     String codePrefix,
                     Integer httpStatus,
                     Boolean retryable,
                     boolean isAbstract) {
    this.name = name;
    this.parent = parent;
    this.codePrefix = codePrefix;
    this.httpStatus = httpStatus;
    this.retryable = retryable;
    this.isAbstract = isAbstract;
  }

  public String getName() {
    return name;
  }

  public String getParent() {
    return parent;
  }

  public String getCodePrefix() {
    return codePrefix;
  }

  public Integer getHttpStatus() {
    return httpStatus;
  }

  public Boolean getRetryable() {
    return retryable;
  }

  public boolean isAbstract() {
    return isAbstract;
  }
}
