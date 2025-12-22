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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.springframework.http.ResponseEntity;
import org.junit.jupiter.api.Test;

public class GeneratedExceptionRuntimeTest {
  @Test
  void generatedExceptionExposesExpectedValues() throws Exception {
    String yaml = "package: com.example.hello\n"
        + "baseException: Hello\n"
        + "source: hello-service\n"
        + "categories:\n"
        + "  Common:\n"
        + "    codePrefix: CM\n"
        + "errors:\n"
        + "  helloWorld:\n"
        + "    category: Common\n"
        + "    fixed:\n"
        + "      code: 1\n"
        + "      description: \"Hello {name}\"\n"
        + "      details: \"Hello detail {name}\"\n"
        + "    required:\n"
        + "      name: String\n";

    Path spec = Files.createTempFile("edl-test", ".yaml");
    Files.writeString(spec, yaml, StandardCharsets.UTF_8);
    Path outputDir = Files.createTempDirectory("edl-generated");

    EdlCompiler compiler = new EdlCompiler();
    CompilationResult result = compiler.compile(spec, outputDir, new CompilerOptions(false, false));
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
      Method getDescriptionTemplate = exceptionClass.getMethod("descriptionTemplate");
      Method getDetailTemplate = exceptionClass.getMethod("detailTemplate");
      Method getDescription = exceptionClass.getMethod("description");
      Method getDetail = exceptionClass.getMethod("detail");
      Method getDetails = exceptionClass.getMethod("details");
      Method getErrorInfo = exceptionClass.getMethod("errorInfo");
      Method getSource = exceptionClass.getMethod("source");
      Method getRecoverable = exceptionClass.getMethod("recoverable");

      assertEquals("CM0001", getCode.invoke(exception));
      assertEquals("Hello {name}", getDescriptionTemplate.invoke(exception));
      assertEquals("Hello detail {name}", getDetailTemplate.invoke(exception));
      assertEquals("Hello Ada", getDescription.invoke(exception));
      assertEquals("Hello detail Ada", getDetail.invoke(exception));
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
      expected.put("detail", "Hello detail Ada");
      expected.put("details", "Hello detail Ada");
      expected.put("recoverable", false);
      assertEquals(sortedMap(expected), sortedMap(errorInfo));
    } finally {
      classLoader.close();
    }
  }

  @Test
  void containerResponseRendersErrorList() throws Exception {
    Path sample = Path.of("src", "test", "resources", "samples", "catalog.yaml");
    Path outputDir = Files.createTempDirectory("edl-generated");

    EdlCompiler compiler = new EdlCompiler();
    CompilationResult result = compiler.compile(sample, outputDir, new CompilerOptions(false, true));
    assertTrue(result.getDiagnostics().stream().noneMatch(d -> d.getSeverity() == DiagnosticSeverity.ERROR),
        formatDiagnostics(result.getDiagnostics()));

    Path classesDir = Files.createTempDirectory("edl-classes");
    compileSources(outputDir, classesDir);

    URLClassLoader classLoader = new URLClassLoader(new URL[] { classesDir.toUri().toURL() },
        GeneratedExceptionRuntimeTest.class.getClassLoader());
    try {
      Class<?> containerClass = classLoader.loadClass("com.example.catalog.AuthContainerException");
      Class<?> errorClass = classLoader.loadClass("com.example.catalog.FooErrorException");
      Class<?> catalogExceptionClass = classLoader.loadClass("com.example.catalog.CatalogException");
      Class<?> handlerClass = classLoader.loadClass("com.example.catalog.CatalogExceptionHandler");

      Object container = containerClass.getConstructor().newInstance();
      containerClass.getMethod("add", catalogExceptionClass)
          .invoke(container, buildFooError(errorClass, "Ada"));
      containerClass.getMethod("add", catalogExceptionClass)
          .invoke(container, buildFooError(errorClass, "Bob"));

      Object handler = handlerClass.getConstructor().newInstance();
      Object response = handlerClass.getMethod("handleAuthContainerException", containerClass)
          .invoke(handler, container);

      @SuppressWarnings("unchecked")
      ResponseEntity<Map<String, Object>> entity = (ResponseEntity<Map<String, Object>>) response;
      Map<String, Object> body = entity.getBody();
      assertNotNull(body, "Response body should not be null");

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> errors = (List<Map<String, Object>>) body.get("Error");
      assertNotNull(errors, "Error list should be present");
      assertEquals(2, errors.size());

      Map<String, Object> expectedAda = new HashMap<>();
      expectedAda.put("Source", "catalog-service");
      expectedAda.put("ReasonCode", "AUTH0001");
      expectedAda.put("Description", "Userid invalid Ada");
      expectedAda.put("Details", "Userid Ada does not exist");
      expectedAda.put("Recoverable", false);

      Map<String, Object> expectedBob = new HashMap<>();
      expectedBob.put("Source", "catalog-service");
      expectedBob.put("ReasonCode", "AUTH0001");
      expectedBob.put("Description", "Userid invalid Bob");
      expectedBob.put("Details", "Userid Bob does not exist");
      expectedBob.put("Recoverable", false);

      assertEquals(sortedMap(expectedAda), sortedMap(errors.get(0)));
      assertEquals(sortedMap(expectedBob), sortedMap(errors.get(1)));
    } finally {
      classLoader.close();
    }
  }

  private Object buildFooError(Class<?> errorClass, String userId) throws Exception {
    Object builder = errorClass.getMethod("builder").invoke(null);
    builder.getClass().getMethod("userId", String.class).invoke(builder, userId);
    builder.getClass().getMethod("region", String.class).invoke(builder, "us-east-1");
    return builder.getClass().getMethod("build").invoke(builder);
  }

  private Map<String, Object> sortedMap(Map<String, Object> input) {
    return input.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()),
            LinkedHashMap::putAll);
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
    options.add("-classpath");
    options.add(System.getProperty("java.class.path"));
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
