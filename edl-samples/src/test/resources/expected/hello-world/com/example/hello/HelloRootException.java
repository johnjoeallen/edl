package com.example.hello;

import java.lang.Object;
import java.lang.RuntimeException;
import java.lang.String;
import java.lang.Throwable;
import java.util.Map;
import java.util.Objects;

public abstract class HelloRootException extends RuntimeException {
  private static final String SOURCE = "hello-service";

  private final String code;

  private final String messageTemplate;

  private final Map<String, Object> details;

  protected HelloRootException(String code, String messageTemplate, Map<String, Object> details,
      Throwable cause) {
    super(messageTemplate, cause);
    this.code = Objects.requireNonNull(code, "code");
    this.messageTemplate = Objects.requireNonNull(messageTemplate, "messageTemplate");
    this.details = Map.copyOf(Objects.requireNonNull(details, "details"));
  }

  public String code() {
    return code;
  }

  public String messageTemplate() {
    return messageTemplate;
  }

  public Map<String, Object> details() {
    return details;
  }

  public String source() {
    return SOURCE;
  }

  public Map<String, Object> errorInfo() {
    return Map.ofEntries(
          Map.entry("source", SOURCE),
          Map.entry("code", code),
          Map.entry("description", renderDescription(messageTemplate, details)),
          Map.entry("details", details),
          Map.entry("recoverable", recoverable())
        );
  }

  public boolean recoverable() {
    return false;
  }

  private static String renderDescription(String template, Map<String, Object> details) {
    String resolved = template;
    for (Map.Entry<String, Object> entry : details.entrySet()) {
      resolved = resolved.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
    }
    return resolved;
  }
}
