package com.edl.core;

public final class CompilerOptions {
  private final boolean generateDocs;

  public CompilerOptions(boolean generateDocs) {
    this.generateDocs = generateDocs;
  }

  public boolean isGenerateDocs() {
    return generateDocs;
  }
}
