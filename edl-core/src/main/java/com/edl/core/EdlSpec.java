package com.edl.core;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EdlSpec {
  private final String packageName;
  private final String baseException;
  private final String source;
  private final Map<String, Object> options;
  private final String containerWrapperKey;
  private final String containerItemKey;
  private final LinkedHashMap<String, String> responseFields;
  private final LinkedHashMap<String, CategoryDef> categories;
  private final LinkedHashMap<String, ErrorDef> errors;

  public EdlSpec(String packageName,
                 String baseException,
                 String source,
                 Map<String, Object> options,
                 String containerWrapperKey,
                 String containerItemKey,
                 LinkedHashMap<String, String> responseFields,
                 LinkedHashMap<String, CategoryDef> categories,
                 LinkedHashMap<String, ErrorDef> errors) {
    this.packageName = packageName;
    this.baseException = baseException;
    this.source = source;
    this.options = options;
    this.containerWrapperKey = containerWrapperKey;
    this.containerItemKey = containerItemKey;
    this.responseFields = responseFields;
    this.categories = categories;
    this.errors = errors;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getBaseException() {
    return baseException;
  }

  public String getSource() {
    return source;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  public String getContainerWrapperKey() {
    return containerWrapperKey;
  }

  public String getContainerItemKey() {
    return containerItemKey;
  }

  public LinkedHashMap<String, String> getResponseFields() {
    return responseFields;
  }

  public LinkedHashMap<String, CategoryDef> getCategories() {
    return categories;
  }

  public LinkedHashMap<String, ErrorDef> getErrors() {
    return errors;
  }
}
