package com.edl.core;

public final class NameUtils {
  private NameUtils() {
  }

  public static String toPascalCase(String camelCase) {
    if (camelCase == null || camelCase.isEmpty()) {
      return camelCase;
    }
    return Character.toUpperCase(camelCase.charAt(0)) + camelCase.substring(1);
  }
}
