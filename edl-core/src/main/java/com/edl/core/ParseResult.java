package com.edl.core;

import java.util.List;

public final class ParseResult {
  private final EdlSpec spec;
  private final List<Diagnostic> diagnostics;

  public ParseResult(EdlSpec spec, List<Diagnostic> diagnostics) {
    this.spec = spec;
    this.diagnostics = diagnostics;
  }

  public EdlSpec getSpec() {
    return spec;
  }

  public List<Diagnostic> getDiagnostics() {
    return diagnostics;
  }
}
