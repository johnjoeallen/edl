package com.edl.samples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.edl.core.CompilationResult;
import com.edl.core.CompilerOptions;
import com.edl.core.Diagnostic;
import com.edl.core.DiagnosticSeverity;
import com.edl.core.EdlCompiler;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

public class GeneratedExceptionRuntimeTest {
  @Test
  void generatedExceptionExposesExpectedValues() throws Exception {
    String yaml = "package: com.example.hello\n"
        + "rootException: HelloRootException\n"
        + "source: hello-service\n"
        + "categories:\n"
        + "  Common:\n"
        + "    codePrefix: CM\n"
        + "errors:\n"
        + "  helloWorld:\n"
        + "    category: Common\n"
        + "    code: 1\n"
        + "    message: \"Hello {name}\"\n"
        + "    params:\n"
        + "      name: String\n"
        + "    requiredParams:\n"
        + "      - name\n";

    Path spec = Files.createTempFile("edl-test", ".yml");
    Files.writeString(spec, yaml, StandardCharsets.UTF_8);
    Path outputDir = Files.createTempDirectory("edl-generated");

    EdlCompiler compiler = new EdlCompiler();
    CompilationResult result = compiler.compile(spec, outputDir, new CompilerOptions(false));
    assertTrue(result.getDiagnostics().stream().noneMatch(d -> d.getSeverity() == DiagnosticSeverity.ERROR),
        formatDiagnostics(result.getDiagnostics()));

    Path classesDir = Files.createTempDirectory("edl-classes");
    compileSources(outputDir, classesDir);

    URLClassLoader classLoader = new URLClassLoader(new URL[] { classesDir.toUri().toURL() });
    try {
      Class<?> exceptionClass = classLoader.loadClass("com.example.hello.HelloWorldException");
      Method builderMethod = exceptionClass.getMethod("builder");
      Object builder = builderMethod.invoke(null);
      Method nameSetter = builder.getClass().getMethod("name", String.class);
      nameSetter.invoke(builder, "Ada");
      Method buildMethod = builder.getClass().getMethod("build");
      Object exception = buildMethod.invoke(builder);

      Method getCode = exceptionClass.getMethod("code");
      Method getMessageTemplate = exceptionClass.getMethod("messageTemplate");
      Method getDetails = exceptionClass.getMethod("details");
      Method getErrorInfo = exceptionClass.getMethod("errorInfo");
      Method getSource = exceptionClass.getMethod("source");
      Method getRecoverable = exceptionClass.getMethod("recoverable");

      assertEquals("CM0001", getCode.invoke(exception));
      assertEquals("Hello {name}", getMessageTemplate.invoke(exception));
      assertEquals("hello-service", getSource.invoke(exception));
      assertEquals(false, getRecoverable.invoke(exception));

      @SuppressWarnings("unchecked")
      Map<String, Object> details = (Map<String, Object>) getDetails.invoke(exception);
      assertEquals(Map.of("name", "Ada"), details);

      @SuppressWarnings("unchecked")
      Map<String, Object> errorInfo = (Map<String, Object>) getErrorInfo.invoke(exception);
      Map<String, Object> expected = new HashMap<>();
      expected.put("source", "hello-service");
      expected.put("code", "CM0001");
      expected.put("description", "Hello Ada");
      expected.put("details", Map.of("name", "Ada"));
      expected.put("recoverable", false);
      assertEquals(expected, errorInfo);
    } finally {
      classLoader.close();
    }
  }

  private void compileSources(Path sourceRoot, Path classesDir) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "JDK compiler required for generated source test");

    List<Path> sources;
    try (Stream<Path> stream = Files.walk(sourceRoot)) {
      sources = stream
          .filter(path -> path.toString().endsWith(".java"))
          .sorted(Comparator.comparing(Path::toString))
          .collect(Collectors.toList());
    }

    List<String> options = new ArrayList<>();
    options.add("--release");
    options.add("17");
    options.add("-d");
    options.add(classesDir.toString());

    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
      Iterable<? extends javax.tools.JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(sources);
      JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, units);
      Boolean ok = task.call();
      assertTrue(Boolean.TRUE.equals(ok), "Generated sources failed to compile");
    }
  }

  private String formatDiagnostics(List<Diagnostic> diagnostics) {
    List<String> lines = new ArrayList<>();
    for (Diagnostic diagnostic : diagnostics) {
      lines.add(diagnostic.format());
    }
    return String.join("\n", lines);
  }
}
