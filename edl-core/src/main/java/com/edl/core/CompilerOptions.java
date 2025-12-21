package com.edl.core;

public final class CompilerOptions {
  private final boolean generateDocs;
  private final boolean generateSpringHandler;

  public CompilerOptions(boolean generateDocs, boolean generateSpringHandler) {
    this.generateDocs = generateDocs;
    this.generateSpringHandler = generateSpringHandler;
  }

  public boolean isGenerateDocs() {
    return generateDocs;
  }

  public boolean isGenerateSpringHandler() {
    return generateSpringHandler;
  }
}
