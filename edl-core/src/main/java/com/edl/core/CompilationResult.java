package com.edl.core;

import java.nio.file.Path;
import java.util.List;

public final class CompilationResult {
  private final List<Path> generatedFiles;
  private final List<Diagnostic> diagnostics;

  public CompilationResult(List<Path> generatedFiles, List<Diagnostic> diagnostics) {
    this.generatedFiles = generatedFiles;
    this.diagnostics = diagnostics;
  }

  public List<Path> getGeneratedFiles() {
    return generatedFiles;
  }

  public List<Diagnostic> getDiagnostics() {
    return diagnostics;
  }
}
