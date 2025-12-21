package com.edl.samples;

import com.edl.core.CompilationResult;
import com.edl.core.CompilerOptions;
import com.edl.core.Diagnostic;
import com.edl.core.DiagnosticSeverity;
import com.edl.core.EdlCompiler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class SamplesGenerator {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      throw new IllegalArgumentException("Expected args: <samplesDir> <outputDir>");
    }
    Path samplesDir = Path.of(args[0]);
    Path outputDir = Path.of(args[1]);
    generate(samplesDir, outputDir);
  }

  private static void generate(Path samplesDir, Path outputDir) throws IOException {
    if (!Files.isDirectory(samplesDir)) {
      throw new IllegalArgumentException("Samples directory not found: " + samplesDir);
    }

    EdlCompiler compiler = new EdlCompiler();
    try (Stream<Path> stream = Files.list(samplesDir)) {
      List<Path> specs = stream
          .filter(path -> path.getFileName().toString().endsWith(".yaml"))
          .sorted(Comparator.comparing(path -> path.getFileName().toString()))
          .toList();

      for (Path spec : specs) {
        String name = stripExtension(spec.getFileName().toString());
        Path specOutput = outputDir.resolve(name);
        CompilationResult result = compiler.compile(spec, specOutput, new CompilerOptions(false, true));
        List<Diagnostic> errors = result.getDiagnostics().stream()
            .filter(diagnostic -> diagnostic.getSeverity() == DiagnosticSeverity.ERROR)
            .toList();
        if (!errors.isEmpty()) {
          throw new IllegalStateException("Failed to compile " + spec + ":\n" + format(errors));
        }
      }
    }
  }

  private static String stripExtension(String name) {
    int idx = name.lastIndexOf('.');
    if (idx > 0) {
      return name.substring(0, idx);
    }
    return name;
  }

  private static String format(List<Diagnostic> diagnostics) {
    StringBuilder builder = new StringBuilder();
    for (Diagnostic diagnostic : diagnostics) {
      builder.append(diagnostic.format()).append("\n");
    }
    return builder.toString();
  }
}
