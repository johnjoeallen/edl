package com.edl.core;

public final class Diagnostic {
  private final DiagnosticSeverity severity;
  private final String message;
  private final String path;
  private final String file;
  private final Integer line;
  private final Integer column;

  public Diagnostic(DiagnosticSeverity severity,
                    String message,
                    String path,
                    String file,
                    Integer line,
                    Integer column) {
    this.severity = severity;
    this.message = message;
    this.path = path;
    this.file = file;
    this.line = line;
    this.column = column;
  }

  public DiagnosticSeverity getSeverity() {
    return severity;
  }

  public String getMessage() {
    return message;
  }

  public String getPath() {
    return path;
  }

  public String getFile() {
    return file;
  }

  public Integer getLine() {
    return line;
  }

  public Integer getColumn() {
    return column;
  }

  public String format() {
    StringBuilder builder = new StringBuilder();
    builder.append(severity).append(": ").append(message);
    if (file != null && !file.isBlank()) {
      builder.append(" in ").append(file);
    }
    if (path != null && !path.isBlank()) {
      builder.append(" at ").append(path);
    }
    if (line != null && column != null) {
      builder.append(" (line ").append(line).append(", col ").append(column).append(")");
    }
    return builder.toString();
  }
}
