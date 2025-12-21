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
  private static final List<String> DEFAULT_CORE_PARAMS =
      List.of("source", "code", "description", "detail", "details", "recoverable");
  private static final Set<String> DERIVED_PARAMS =
      Set.of("source", "code", "description", "detail", "details", "recoverable");
  private static final Set<String> RENDERABLE_DERIVED_PARAMS =
      Set.of("source", "code", "recoverable");

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
    String rootName = spec.getRootException();
    if (rootName != null) {
      if (rootName.endsWith("Exception")) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
            "rootException should not include the 'Exception' suffix",
            "rootException", file, marks));
      }
      if (!CATEGORY_PATTERN.matcher(rootName).matches()) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
            "rootException must be PascalCase", "rootException", file, marks));
      }
    }
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
            "errors." + error.getName() + ".fixed.code", file, marks));
      }

      CategoryDef category = spec.getCategories().get(categoryName);
      Set<String> coreParams = category == null ? Set.of() : category.getParams().keySet();
      Set<String> requiredParams = error.getRequiredParams().keySet();
      Set<String> optionalParams = error.getOptionalParams().keySet();
      for (String param : requiredParams) {
        if (DERIVED_PARAMS.contains(param)) {
          diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
              "Param '" + param + "' is reserved for derived values",
              "errors." + error.getName() + ".required." + param, file, marks));
        } else if (coreParams.contains(param)) {
          diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
              "Param '" + param + "' duplicates category param",
              "errors." + error.getName() + ".required." + param, file, marks));
        }
      }
      for (String param : optionalParams) {
        if (DERIVED_PARAMS.contains(param)) {
          diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
              "Param '" + param + "' is reserved for derived values",
              "errors." + error.getName() + ".optional." + param, file, marks));
        } else if (coreParams.contains(param) || requiredParams.contains(param)) {
          diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
              "Param '" + param + "' duplicates another param",
              "errors." + error.getName() + ".optional." + param, file, marks));
        }
      }
    }
  }

  private void validateMessageTemplates(EdlSpec spec, List<Diagnostic> diagnostics, String file, Map<String, Mark> marks) {
    for (ErrorDef error : spec.getErrors().values()) {
      CategoryDef category = spec.getCategories().get(error.getCategory());
      Set<String> allowedParams = new HashSet<>();
      if (category != null) {
        allowedParams.addAll(category.getParams().keySet());
      }
      allowedParams.addAll(error.getRequiredParams().keySet());
      allowedParams.addAll(error.getOptionalParams().keySet());
      allowedParams.addAll(RENDERABLE_DERIVED_PARAMS);

      validateTemplatePlaceholders(error.getDescription(), allowedParams,
          "errors." + error.getName() + ".description", diagnostics, file, marks);
      validateTemplatePlaceholders(error.getDetail(), allowedParams,
          "errors." + error.getName() + ".detail", diagnostics, file, marks);

    }
  }

  private void validateResponseFields(EdlSpec spec, List<Diagnostic> diagnostics, String file, Map<String, Mark> marks) {
    Set<String> allowed = new HashSet<>(DEFAULT_CORE_PARAMS);
    for (CategoryDef category : spec.getCategories().values()) {
      if (category.getParams().isEmpty()) {
        allowed.addAll(DEFAULT_CORE_PARAMS);
      } else {
        allowed.addAll(category.getParams().keySet());
      }
    }
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
  }

  private void validateTemplatePlaceholders(String template,
                                            Set<String> allowedParams,
                                            String path,
                                            List<Diagnostic> diagnostics,
                                            String file,
                                            Map<String, Mark> marks) {
    Set<String> placeholders = new HashSet<>();
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
    while (matcher.find()) {
      placeholders.add(matcher.group(1));
    }
    for (String placeholder : placeholders) {
      if (!allowedParams.contains(placeholder)) {
        diagnostics.add(diagnostic(DiagnosticSeverity.ERROR,
            "Template placeholder '" + placeholder + "' is missing a param",
            path, file, marks));
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
