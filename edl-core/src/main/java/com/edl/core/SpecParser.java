package com.edl.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.error.Mark;

public final class SpecParser {
  public ParseResult parse(Path sourcePath, Object data, Map<String, Mark> marks) {
    List<Diagnostic> diagnostics = new ArrayList<>();
    String file = sourcePath == null ? null : sourcePath.toString();
    if (!(data instanceof Map<?, ?> rawMap)) {
      diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Top level YAML must be a map", "", file, marks));
      return new ParseResult(null, diagnostics);
    }

    LinkedHashMap<String, Object> map = toLinkedMap(rawMap, diagnostics, file, marks, "");
    String packageName = readString(map, diagnostics, file, marks, "package", true);
    String rootException = readString(map, diagnostics, file, marks, "rootException", true);
    String source = readString(map, diagnostics, file, marks, "source", true);
    Map<String, Object> options = readMap(map, diagnostics, file, marks, "options", false);
    LinkedHashMap<String, CategoryDef> categories = readCategories(map, diagnostics, file, marks);
    LinkedHashMap<String, ErrorDef> errors = readErrors(map, diagnostics, file, marks);

    if (packageName == null || rootException == null || source == null || categories == null || errors == null) {
      return new ParseResult(null, diagnostics);
    }

    EdlSpec spec = new EdlSpec(packageName, rootException, source, options, categories, errors);
    return new ParseResult(spec, diagnostics);
  }

  private LinkedHashMap<String, CategoryDef> readCategories(Map<String, Object> map,
                                                           List<Diagnostic> diagnostics,
                                                           String file,
                                                           Map<String, Mark> marks) {
    Object raw = map.get("categories");
    if (raw == null) {
      diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Missing required key 'categories'", "categories", file, marks));
      return null;
    }
    if (!(raw instanceof Map<?, ?> rawMap)) {
      diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "categories must be a map", "categories", file, marks));
      return null;
    }
    LinkedHashMap<String, Object> categoriesMap = toLinkedMap(rawMap, diagnostics, file, marks, "categories");
    LinkedHashMap<String, CategoryDef> categories = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : categoriesMap.entrySet()) {
      String name = entry.getKey();
      String path = "categories." + name;
      if (!(entry.getValue() instanceof Map<?, ?> rawCategory)) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Category must be a map", path, file, marks));
        continue;
      }
      LinkedHashMap<String, Object> categoryMap = toLinkedMap(rawCategory, diagnostics, file, marks, path);
      String parent = readString(categoryMap, diagnostics, file, marks, path + ".parent", false);
      String codePrefix = readString(categoryMap, diagnostics, file, marks, path + ".codePrefix", true);
      Integer httpStatus = readInteger(categoryMap, diagnostics, file, marks, path + ".httpStatus", false);
      Boolean retryable = readBoolean(categoryMap, diagnostics, file, marks, path + ".retryable", false);
      Boolean abstractFlag = readBoolean(categoryMap, diagnostics, file, marks, path + ".abstract", false);
      boolean isAbstract = abstractFlag == null || abstractFlag;
      if (codePrefix == null) {
        continue;
      }
      categories.put(name, new CategoryDef(name, parent, codePrefix, httpStatus, retryable, isAbstract));
    }
    return categories;
  }

  private LinkedHashMap<String, ErrorDef> readErrors(Map<String, Object> map,
                                                     List<Diagnostic> diagnostics,
                                                     String file,
                                                     Map<String, Mark> marks) {
    Object raw = map.get("errors");
    if (raw == null) {
      diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Missing required key 'errors'", "errors", file, marks));
      return null;
    }
    if (!(raw instanceof Map<?, ?> rawMap)) {
      diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "errors must be a map", "errors", file, marks));
      return null;
    }
    LinkedHashMap<String, Object> errorsMap = toLinkedMap(rawMap, diagnostics, file, marks, "errors");
    LinkedHashMap<String, ErrorDef> errors = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : errorsMap.entrySet()) {
      String name = entry.getKey();
      String path = "errors." + name;
      if (!(entry.getValue() instanceof Map<?, ?> rawError)) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Error must be a map", path, file, marks));
        continue;
      }
      LinkedHashMap<String, Object> errorMap = toLinkedMap(rawError, diagnostics, file, marks, path);
      String category = readString(errorMap, diagnostics, file, marks, path + ".category", true);
      Object codeValue = errorMap.get("code");
      String message = readString(errorMap, diagnostics, file, marks, path + ".message", true);
      LinkedHashMap<String, String> params = readStringMap(errorMap, diagnostics, file, marks, path + ".params", true);
      List<String> requiredParams = readStringList(errorMap, diagnostics, file, marks, path + ".requiredParams", false);
      Boolean recoverable = readBoolean(errorMap, diagnostics, file, marks, path + ".recoverable", false);
      if (category == null || message == null || params == null || codeValue == null) {
        if (codeValue == null) {
          diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Missing required key 'code'", path + ".code", file, marks));
        }
        continue;
      }
      String numericCode = coerceCode(codeValue, diagnostics, file, marks, path + ".code");
      if (numericCode == null) {
        continue;
      }
      if (requiredParams == null) {
        requiredParams = List.of();
      }
      boolean isRecoverable = recoverable != null && recoverable;
      errors.put(name, new ErrorDef(name, category, numericCode, message, params, requiredParams, isRecoverable));
    }
    return errors;
  }

  private String coerceCode(Object value,
                            List<Diagnostic> diagnostics,
                            String file,
                            Map<String, Mark> marks,
                            String path) {
    if (value instanceof Integer integerValue) {
      if (integerValue < 0 || integerValue > 9999) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Error code must be between 0 and 9999", path, file, marks));
        return null;
      }
      return String.format("%04d", integerValue);
    }
    if (value instanceof String stringValue) {
      if (!stringValue.matches("\\d{1,4}")) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Error code must be 1 to 4 digits", path, file, marks));
        return null;
      }
      int parsed = Integer.parseInt(stringValue);
      if (parsed < 0 || parsed > 9999) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Error code must be between 0 and 9999", path, file, marks));
        return null;
      }
      return String.format("%04d", parsed);
    }
    diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Error code must be a number or string", path, file, marks));
    return null;
  }

  private String readString(Map<String, Object> map,
                            List<Diagnostic> diagnostics,
                            String file,
                            Map<String, Mark> marks,
                            String path,
                            boolean required) {
    Object value = map.get(lastSegment(path));
    if (value == null) {
      if (required) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Missing required key '" + lastSegment(path) + "'", path, file, marks));
      }
      return null;
    }
    if (value instanceof String stringValue) {
      return stringValue;
    }
    diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Expected string for '" + lastSegment(path) + "'", path, file, marks));
    return null;
  }

  private Integer readInteger(Map<String, Object> map,
                              List<Diagnostic> diagnostics,
                              String file,
                              Map<String, Mark> marks,
                              String path,
                              boolean required) {
    Object value = map.get(lastSegment(path));
    if (value == null) {
      if (required) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Missing required key '" + lastSegment(path) + "'", path, file, marks));
      }
      return null;
    }
    if (value instanceof Integer intValue) {
      return intValue;
    }
    if (value instanceof String stringValue) {
      try {
        return Integer.parseInt(stringValue);
      } catch (NumberFormatException ignored) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Expected integer for '" + lastSegment(path) + "'", path, file, marks));
        return null;
      }
    }
    diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Expected integer for '" + lastSegment(path) + "'", path, file, marks));
    return null;
  }

  private Boolean readBoolean(Map<String, Object> map,
                              List<Diagnostic> diagnostics,
                              String file,
                              Map<String, Mark> marks,
                              String path,
                              boolean required) {
    Object value = map.get(lastSegment(path));
    if (value == null) {
      if (required) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Missing required key '" + lastSegment(path) + "'", path, file, marks));
      }
      return null;
    }
    if (value instanceof Boolean boolValue) {
      return boolValue;
    }
    if (value instanceof String stringValue) {
      if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
        return Boolean.parseBoolean(stringValue);
      }
    }
    diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Expected boolean for '" + lastSegment(path) + "'", path, file, marks));
    return null;
  }

  private Map<String, Object> readMap(Map<String, Object> map,
                                      List<Diagnostic> diagnostics,
                                      String file,
                                      Map<String, Mark> marks,
                                      String key,
                                      boolean required) {
    Object value = map.get(key);
    if (value == null) {
      if (required) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Missing required key '" + key + "'", key, file, marks));
      }
      return Map.of();
    }
    if (value instanceof Map<?, ?> rawMap) {
      return toLinkedMap(rawMap, diagnostics, file, marks, key);
    }
    diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Expected map for '" + key + "'", key, file, marks));
    return Map.of();
  }

  private LinkedHashMap<String, String> readStringMap(Map<String, Object> map,
                                                      List<Diagnostic> diagnostics,
                                                      String file,
                                                      Map<String, Mark> marks,
                                                      String path,
                                                      boolean required) {
    Object value = map.get(lastSegment(path));
    if (value == null) {
      if (required) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Missing required key '" + lastSegment(path) + "'", path, file, marks));
      }
      return null;
    }
    if (!(value instanceof Map<?, ?> rawMap)) {
      diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Expected map for '" + lastSegment(path) + "'", path, file, marks));
      return null;
    }
    LinkedHashMap<String, Object> objectMap = toLinkedMap(rawMap, diagnostics, file, marks, path);
    LinkedHashMap<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
      if (entry.getValue() instanceof String stringValue) {
        result.put(entry.getKey(), stringValue);
      } else {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Parameter types must be strings", path + "." + entry.getKey(), file, marks));
      }
    }
    return result;
  }

  private List<String> readStringList(Map<String, Object> map,
                                      List<Diagnostic> diagnostics,
                                      String file,
                                      Map<String, Mark> marks,
                                      String path,
                                      boolean required) {
    Object value = map.get(lastSegment(path));
    if (value == null) {
      if (required) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Missing required key '" + lastSegment(path) + "'", path, file, marks));
      }
      return required ? null : List.of();
    }
    if (!(value instanceof List<?> rawList)) {
      diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Expected list for '" + lastSegment(path) + "'", path, file, marks));
      return null;
    }
    List<String> result = new ArrayList<>();
    for (Object item : rawList) {
      if (item instanceof String stringValue) {
        result.add(stringValue);
      } else {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Required params must be strings", path, file, marks));
        return null;
      }
    }
    return result;
  }

  private LinkedHashMap<String, Object> toLinkedMap(Map<?, ?> raw,
                                                    List<Diagnostic> diagnostics,
                                                    String file,
                                                    Map<String, Mark> marks,
                                                    String path) {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : raw.entrySet()) {
      if (!(entry.getKey() instanceof String key)) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Map keys must be strings", path, file, marks));
        continue;
      }
      map.put(key, entry.getValue());
    }
    return map;
  }

  private String lastSegment(String path) {
    int idx = path.lastIndexOf('.');
    return idx >= 0 ? path.substring(idx + 1) : path;
  }

  private Diagnostic diagnostic(DiagnosticSeverity severity,
                                String message,
                                String path,
                                String file,
                                Map<String, Mark> marks) {
    Mark mark = marks.get(path);
    Integer line = mark == null ? null : mark.getLine() + 1;
    Integer column = mark == null ? null : mark.getColumn() + 1;
    return new Diagnostic(severity, message, path, file, line, column);
  }
}
