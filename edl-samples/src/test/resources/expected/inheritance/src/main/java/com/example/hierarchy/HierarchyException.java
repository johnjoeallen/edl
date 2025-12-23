package com.example.hierarchy;

import java.lang.Object;
import java.lang.RuntimeException;
import java.lang.String;
import java.lang.Throwable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public abstract class HierarchyException extends RuntimeException {
  private static final String SOURCE = "hierarchy-service";

  private final String code;

  private final String descriptionTemplate;

  private final String detailTemplate;

  private final Map<String, Object> details;

  private final int httpStatus;

  protected HierarchyException(String code, int httpStatus, String descriptionTemplate,
      String detailTemplate, Map<String, Object> details, Throwable cause) {
    super(descriptionTemplate, cause);
    this.code = Objects.requireNonNull(code, "code");
    this.descriptionTemplate = Objects.requireNonNull(descriptionTemplate, "descriptionTemplate");
    this.detailTemplate = Objects.requireNonNull(detailTemplate, "detailTemplate");
    this.details = Map.copyOf(Objects.requireNonNull(details, "details"));
    this.httpStatus = httpStatus;
  }

  public String code() {
    return code;
  }

  public String descriptionTemplate() {
    return descriptionTemplate;
  }

  public String description() {
    return renderTemplate(descriptionTemplate, renderValues());
  }

  public String detailTemplate() {
    return detailTemplate;
  }

  public String detail() {
    return renderTemplate(detailTemplate, renderValues());
  }

  public Map<String, Object> details() {
    return details;
  }

  public String source() {
    return SOURCE;
  }

  public Map<String, Object> errorInfo() {
    return coreValues();
  }

  public boolean recoverable() {
    return false;
  }

  public int httpStatus() {
    return httpStatus;
  }

  protected abstract Map<String, Object> coreValues();

  private Map<String, Object> renderValues() {
    Map<String, Object> values = new LinkedHashMap<>(details);
    values.put("source", SOURCE);
    values.put("code", code);
    values.put("recoverable", recoverable());
    return values;
  }

  private static String renderTemplate(String template, Map<String, Object> values) {
    String resolved = template;
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      resolved = resolved.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
    }
    return resolved;
  }
}
