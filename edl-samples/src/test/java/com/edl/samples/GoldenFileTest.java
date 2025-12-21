package com.edl.samples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.edl.core.CompilationResult;
import com.edl.core.CompilerOptions;
import com.edl.core.Diagnostic;
import com.edl.core.DiagnosticSeverity;
import com.edl.core.EdlCompiler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class GoldenFileTest {
  @Test
  void helloWorldSampleMatchesGoldenFiles() throws Exception {
    assertGolden("hello-world");
  }

  @Test
  void inheritanceSampleMatchesGoldenFiles() throws Exception {
    assertGolden("inheritance");
  }

  @Test
  void catalogSampleMatchesGoldenFiles() throws Exception {
    assertGolden("catalog");
  }

  private void assertGolden(String sampleName) throws Exception {
    Path sample = Path.of("src", "test", "resources", "samples", sampleName + ".yml");
    Path expectedDir = Path.of("src", "test", "resources", "expected", sampleName);
    Path outputDir = Files.createTempDirectory("edl-samples");

    EdlCompiler compiler = new EdlCompiler();
    CompilationResult result = compiler.compile(sample, outputDir, new CompilerOptions(false, true));

    List<Diagnostic> errors = result.getDiagnostics().stream()
        .filter(d -> d.getSeverity() == DiagnosticSeverity.ERROR)
        .toList();
    assertTrue(errors.isEmpty(), formatDiagnostics(errors));

    Set<Path> expectedFiles = listFiles(expectedDir);
    Set<Path> generatedFiles = listFiles(outputDir);

    Set<Path> expectedRelative = relativize(expectedDir, expectedFiles);
    Set<Path> generatedRelative = relativize(outputDir, generatedFiles);

    assertEquals(expectedRelative, generatedRelative, "Generated file set mismatch for " + sampleName);

    for (Path relative : expectedRelative) {
      Path expected = expectedDir.resolve(relative);
      Path generated = outputDir.resolve(relative);
      String expectedContent = Files.readString(expected, StandardCharsets.UTF_8);
      String generatedContent = Files.readString(generated, StandardCharsets.UTF_8);
      String expectedHash = sha256Hex(normalizeWhitespace(expectedContent));
      String generatedHash = sha256Hex(normalizeWhitespace(generatedContent));
      assertEquals(expectedHash, generatedHash, "Mismatch in " + relative + " for " + sampleName);
    }
  }

  private Set<Path> listFiles(Path root) throws IOException {
    Set<Path> files = new HashSet<>();
    try (var stream = Files.walk(root)) {
      stream.filter(Files::isRegularFile).forEach(files::add);
    }
    return files;
  }

  private Set<Path> relativize(Path root, Set<Path> files) {
    Set<Path> relative = new HashSet<>();
    for (Path file : files) {
      relative.add(root.relativize(file));
    }
    return relative;
  }

  private String formatDiagnostics(List<Diagnostic> diagnostics) {
    List<String> lines = new ArrayList<>();
    for (Diagnostic diagnostic : diagnostics) {
      lines.add(diagnostic.format());
    }
    return String.join("\n", lines);
  }

  private String normalizeWhitespace(String content) {
    return content.replaceAll("\\s+", "");
  }

  private String sha256Hex(String content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compute SHA-256 hash", e);
    }
  }
}
