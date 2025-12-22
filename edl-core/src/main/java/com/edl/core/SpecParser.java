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
    String baseException = readString(map, diagnostics, file, marks, "baseException", true);
    if (map.containsKey("rootException")) {
      diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
          "Use 'baseException' instead of 'rootException'", "rootException", file, marks));
    }
    String source = readString(map, diagnostics, file, marks, "source", true);
    Map<String, Object> options = readMap(map, diagnostics, file, marks, "options", false);
    LinkedHashMap<String, String> responseFields = readStringMap(map, diagnostics, file, marks, "response", false);
    Object containerResponse = readObject(map, diagnostics, file, marks, "containerResponse", false);
    LinkedHashMap<String, CategoryDef> categories = readCategories(map, diagnostics, file, marks);
    LinkedHashMap<String, ErrorDef> errors = readErrors(map, diagnostics, file, marks);

    if (packageName == null || baseException == null || source == null || categories == null || errors == null) {
      return new ParseResult(null, diagnostics);
    }

    if (responseFields == null) {
      responseFields = defaultResponseFields();
    }
    String containerWrapperKey = "errors";
    String containerItemKey = "error";
    Object containerTemplate = containerResponse;
    EdlSpec spec = new EdlSpec(packageName, baseException, source, options,
        containerWrapperKey, containerItemKey, containerTemplate, responseFields, categories, errors);
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
      Boolean containerFlag = readBoolean(categoryMap, diagnostics, file, marks, path + ".container", false);
      LinkedHashMap<String, String> params = readStringMap(categoryMap, diagnostics, file, marks, path + ".params", false);
      boolean isAbstract = abstractFlag == null || abstractFlag;
      boolean isContainer = containerFlag != null && containerFlag;
      if (codePrefix == null) {
        continue;
      }
      if (params == null) {
        params = new LinkedHashMap<>();
      }
      categories.put(name, new CategoryDef(name, parent, codePrefix, httpStatus, retryable, isAbstract, isContainer, params));
    }
    return categories;
  }

  private Object readObject(Map<String, Object> map,
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
    return coerceObject(value, diagnostics, file, marks, path);
  }

  private Object coerceObject(Object value,
                              List<Diagnostic> diagnostics,
                              String file,
                              Map<String, Mark> marks,
                              String path) {
    if (value instanceof Map<?, ?> rawMap) {
      return toLinkedMap(rawMap, diagnostics, file, marks, path);
    }
    if (value instanceof List<?> rawList) {
      List<Object> list = new ArrayList<>();
      for (int i = 0; i < rawList.size(); i++) {
        Object entry = rawList.get(i);
        list.add(coerceObject(entry, diagnostics, file, marks, path + "[" + i + "]"));
      }
      return list;
    }
    if (value instanceof String || value instanceof Number || value instanceof Boolean) {
      return value;
    }
    diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Unsupported value in '" + lastSegment(path) + "'", path, file, marks));
    return null;
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
      Map<String, Object> fixed = readMap(errorMap, diagnostics, file, marks, path + ".fixed", true);
      Object codeValue = fixed == null ? null : fixed.get("code");
      String description = fixed == null ? null : readString(fixed, diagnostics, file, marks, path + ".fixed.description", true);
      String detail = null;
      if (fixed != null) {
        boolean hasDetail = fixed.containsKey("detail");
        boolean hasDetails = fixed.containsKey("details");
        if (hasDetail && hasDetails) {
          diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
              "Use either 'detail' or 'details', not both", path + ".fixed", file, marks));
        }
        if (hasDetail) {
          detail = readString(fixed, diagnostics, file, marks, path + ".fixed.detail", true);
        } else if (hasDetails) {
          detail = readString(fixed, diagnostics, file, marks, path + ".fixed.details", true);
        } else {
          diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
              "Missing required key 'detail' or 'details'", path + ".fixed.detail", file, marks));
        }
      }
      LinkedHashMap<String, String> requiredParams = readStringMap(errorMap, diagnostics, file, marks, path + ".required", false);
      LinkedHashMap<String, String> optionalParams = readStringMapOrList(errorMap, diagnostics, file, marks, path + ".optional", false);
      Boolean recoverable = readBoolean(errorMap, diagnostics, file, marks, path + ".recoverable", false);
      Integer httpStatus = readInteger(errorMap, diagnostics, file, marks, path + ".httpStatus", false);
      if (category == null || description == null || detail == null || codeValue == null) {
        if (codeValue == null) {
          diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Missing required key 'code'", path + ".fixed.code", file, marks));
        }
        continue;
      }
      String numericCode = coerceCode(codeValue, diagnostics, file, marks, path + ".fixed.code");
      if (numericCode == null) {
        continue;
      }
      if (requiredParams == null) {
        requiredParams = new LinkedHashMap<>();
      }
      if (optionalParams == null) {
        optionalParams = new LinkedHashMap<>();
      }
      boolean isRecoverable = recoverable != null && recoverable;
      errors.put(name, new ErrorDef(name, category, numericCode, description, detail, requiredParams, optionalParams, isRecoverable, httpStatus));
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
    Object value = map.get(lastSegment(key));
    if (value == null) {
      if (required) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Missing required key '" + lastSegment(key) + "'", key, file, marks));
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

  private LinkedHashMap<String, String> readStringMapOrList(Map<String, Object> map,
                                                            List<Diagnostic> diagnostics,
                                                            String file,
                                                            Map<String, Mark> marks,
                                                            String path,
                                                            boolean required) {
    Object value = map.get(lastSegment(path));
    if (value == null) {
      if (required) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Missing required key '" + lastSegment(path) + "'", path, file, marks));
        return null;
      }
      return new LinkedHashMap<>();
    }
    if (value instanceof Map<?, ?>) {
      return readStringMap(map, diagnostics, file, marks, path, required);
    }
    if (value instanceof List<?>) {
      List<String> names = readStringList(map, diagnostics, file, marks, path, required);
      if (names == null) {
        return null;
      }
      LinkedHashMap<String, String> result = new LinkedHashMap<>();
      for (String name : names) {
        result.put(name, "String");
      }
      return result;
    }
    diagnostics.add(diagnostic(DiagnosticSeverity.ERROR, "Expected map or list for '" + lastSegment(path) + "'", path, file, marks));
    return null;
  }

  private LinkedHashMap<String, String> defaultResponseFields() {
    LinkedHashMap<String, String> defaults = new LinkedHashMap<>();
    defaults.put("source", "source");
    defaults.put("code", "code");
    defaults.put("description", "description");
    defaults.put("detail", "detail");
    defaults.put("details", "details");
    defaults.put("recoverable", "recoverable");
    return defaults;
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
