package com.edl.core;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EdlSpec {
  private final String packageName;
  private final String rootException;
  private final String source;
  private final Map<String, Object> options;
  private final LinkedHashMap<String, CategoryDef> categories;
  private final LinkedHashMap<String, ErrorDef> errors;

  public EdlSpec(String packageName,
                 String rootException,
                 String source,
                 Map<String, Object> options,
                 LinkedHashMap<String, CategoryDef> categories,
                 LinkedHashMap<String, ErrorDef> errors) {
    this.packageName = packageName;
    this.rootException = rootException;
    this.source = source;
    this.options = options;
    this.categories = categories;
    this.errors = errors;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getRootException() {
    return rootException;
  }

  public String getSource() {
    return source;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  public LinkedHashMap<String, CategoryDef> getCategories() {
    return categories;
  }

  public LinkedHashMap<String, ErrorDef> getErrors() {
    return errors;
  }
}
