package com.edl.core;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.error.Mark;

public final class Validator {
  private static final Pattern CATEGORY_PATTERN = Pattern.compile("[A-Z][A-Za-z0-9]*");
  private static final Pattern ERROR_PATTERN = Pattern.compile("[a-z][A-Za-z0-9]*");
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Za-z][A-Za-z0-9_]*)\\}");

  public ValidationResult validate(EdlSpec spec, Map<String, Mark> marks, Path sourcePath) {
    List<Diagnostic> diagnostics = new ArrayList<>();
    String file = sourcePath == null ? null : sourcePath.toString();
    validateNames(spec, diagnostics, file, marks);
    validateCategories(spec, diagnostics, file, marks);
    validateErrors(spec, diagnostics, file, marks);
    validateMessageTemplates(spec, diagnostics, file, marks);
    validateResponseFields(spec, diagnostics, file, marks);
    return new ValidationResult(diagnostics);
  }

  private void validateNames(EdlSpec spec, List<Diagnostic> diagnostics, String file, Map<String, Mark> marks) {
    for (String categoryName : spec.getCategories().keySet()) {
      if (!CATEGORY_PATTERN.matcher(categoryName).matches()) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
            "Category identifiers must be PascalCase", "categories." + categoryName, file, marks));
      }
    }
    for (String errorName : spec.getErrors().keySet()) {
      if (!ERROR_PATTERN.matcher(errorName).matches()) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
            "Error identifiers must be camelCase", "errors." + errorName, file, marks));
      }
    }
  }

  private void validateCategories(EdlSpec spec, List<Diagnostic> diagnostics, String file, Map<String, Mark> marks) {
    Map<String, String> prefixToCategory = new HashMap<>();
    for (CategoryDef category : spec.getCategories().values()) {
      String prefix = category.getCodePrefix();
      String existing = prefixToCategory.putIfAbsent(prefix, category.getName());
      if (existing != null) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
            "Duplicate code prefix '" + prefix + "' used by categories " + existing + " and " + category.getName(),
            "categories." + category.getName() + ".codePrefix", file, marks));
      }
    }

    for (CategoryDef category : spec.getCategories().values()) {
      String parent = category.getParent();
      if (parent != null && !spec.getCategories().containsKey(parent)) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
            "Unknown parent category '" + parent + "'", "categories." + category.getName() + ".parent", file, marks));
      }
    }

    detectCycles(spec, diagnostics, file, marks);
  }

  private void validateErrors(EdlSpec spec, List<Diagnostic> diagnostics, String file, Map<String, Mark> marks) {
    Map<String, Set<String>> codesByCategory = new HashMap<>();
    for (ErrorDef error : spec.getErrors().values()) {
      String categoryName = error.getCategory();
      if (!spec.getCategories().containsKey(categoryName)) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
            "Unknown category '" + categoryName + "'", "errors." + error.getName() + ".category", file, marks));
        continue;
      }
      Set<String> codes = codesByCategory.computeIfAbsent(categoryName, key -> new HashSet<>());
      if (!codes.add(error.getNumericCode())) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
            "Duplicate numeric code '" + error.getNumericCode() + "' in category " + categoryName,
            "errors." + error.getName() + ".code", file, marks));
      }
    }
  }

  private void validateMessageTemplates(EdlSpec spec, List<Diagnostic> diagnostics, String file, Map<String, Mark> marks) {
    for (ErrorDef error : spec.getErrors().values()) {
      Set<String> placeholders = new HashSet<>();
      Matcher matcher = PLACEHOLDER_PATTERN.matcher(error.getMessage());
      while (matcher.find()) {
        placeholders.add(matcher.group(1));
      }
      Set<String> params = error.getParams().keySet();
      for (String placeholder : placeholders) {
        if (!params.contains(placeholder)) {
          diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
              "Message placeholder '" + placeholder + "' is missing a param",
              "errors." + error.getName() + ".message", file, marks));
        }
      }
      for (String required : error.getRequiredParams()) {
        if (!params.contains(required)) {
          diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
              "Required param '" + required + "' is not declared in params",
              "errors." + error.getName() + ".requiredParams", file, marks));
        }
      }
    }
  }

  private void validateResponseFields(EdlSpec spec, List<Diagnostic> diagnostics, String file, Map<String, Mark> marks) {
    Set<String> allowed = Set.of("source", "code", "description", "recoverable", "details");
    Set<String> seenValues = new HashSet<>();
    for (Map.Entry<String, String> entry : spec.getResponseFields().entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (!allowed.contains(key)) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
            "Unknown response field '" + key + "'", "response." + key, file, marks));
        continue;
      }
      if (value == null || value.trim().isEmpty()) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
            "Response field '" + key + "' must not be blank", "response." + key, file, marks));
        continue;
      }
      if (!seenValues.add(value)) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
            "Response field value '" + value + "' is duplicated", "response." + key, file, marks));
      }
    }
    for (String required : allowed) {
      if (!spec.getResponseFields().containsKey(required)) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
            "Missing response field '" + required + "'", "response." + required, file, marks));
      }
    }
  }

  private void detectCycles(EdlSpec spec, List<Diagnostic> diagnostics, String file, Map<String, Mark> marks) {
    Map<String, String> parentMap = new HashMap<>();
    for (CategoryDef category : spec.getCategories().values()) {
      if (category.getParent() != null) {
        parentMap.put(category.getName(), category.getParent());
      }
    }

    Set<String> visited = new HashSet<>();
    Set<String> inStack = new HashSet<>();
    for (String category : spec.getCategories().keySet()) {
      if (!visited.contains(category)) {
        if (dfsCycle(category, parentMap, visited, inStack)) {
          diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
              "Category inheritance cycle detected at " + category,
              "categories." + category + ".parent", file, marks));
        }
      }
    }
  }

  private boolean dfsCycle(String category,
                           Map<String, String> parentMap,
                           Set<String> visited,
                           Set<String> inStack) {
    visited.add(category);
    inStack.add(category);
    String parent = parentMap.get(category);
    if (parent != null) {
      if (!visited.contains(parent) && dfsCycle(parent, parentMap, visited, inStack)) {
        return true;
      }
      if (inStack.contains(parent)) {
        return true;
      }
    }
    inStack.remove(category);
    return false;
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
