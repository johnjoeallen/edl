package com.edl.core;

import java.util.List;

public final class ValidationResult {
  private final List<Diagnostic> diagnostics;

  public ValidationResult(List<Diagnostic> diagnostics) {
    this.diagnostics = diagnostics;
  }

  public List<Diagnostic> getDiagnostics() {
    return diagnostics;
  }

  public boolean hasErrors() {
    return diagnostics.stream().anyMatch(d -> d.getSeverity() == DiagnosticSeverity.ERROR);
  }

  public boolean hasWarnings() {
    return diagnostics.stream().anyMatch(d -> d.getSeverity() == DiagnosticSeverity.WARNING);
  }
}
