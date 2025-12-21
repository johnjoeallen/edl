package com.edl.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.yaml.snakeyaml.error.YAMLException;

public final class EdlCompiler {
  public CompilationResult compile(Path specFile, Path outputDirectory, CompilerOptions options) throws IOException {
    List<Diagnostic> diagnostics = new ArrayList<>();
    YamlLoader loader = new YamlLoader();
    YamlDocument document;
    try {
      document = loader.load(specFile);
    } catch (YAMLException ex) {
      diagnostics.add(new Diagnostic(DiagnosticSeverity.ERROR,
          "Invalid YAML: " + ex.getMessage(), "", specFile.toString(), null, null));
      return new CompilationResult(List.of(), diagnostics);
    }

    SpecParser parser = new SpecParser();
    ParseResult parseResult = parser.parse(specFile, document.getData(), document.getMarks());
    diagnostics.addAll(parseResult.getDiagnostics());
    EdlSpec spec = parseResult.getSpec();
    if (spec == null) {
      return new CompilationResult(List.of(), diagnostics);
    }

    Validator validator = new Validator();
    ValidationResult validationResult = validator.validate(spec, document.getMarks(), specFile);
    diagnostics.addAll(validationResult.getDiagnostics());
    if (options != null && options.isGenerateSpringHandler()) {
      diagnostics.addAll(validateSpringHandlerRequirements(spec, document.getMarks(), specFile));
    }
    if (validationResult.hasErrors()
        || diagnostics.stream().anyMatch(d -> d.getSeverity() == DiagnosticSeverity.ERROR)) {
      return new CompilationResult(List.of(), diagnostics);
    }

    JavaGenerator generator = new JavaGenerator();
    List<Path> generatedFiles = generator.generate(spec, outputDirectory);
    if (options != null && options.isGenerateDocs()) {
      generatedFiles.add(writeDocs(spec, outputDirectory));
    }
    if (options != null && options.isGenerateSpringHandler()) {
      generatedFiles.add(generator.generateSpringHandler(spec, outputDirectory));
    }
    return new CompilationResult(generatedFiles, diagnostics);
  }

  private List<Diagnostic> validateSpringHandlerRequirements(EdlSpec spec,
                                                             java.util.Map<String, org.yaml.snakeyaml.error.Mark> marks,
                                                             Path specFile) {
    List<Diagnostic> diagnostics = new ArrayList<>();
    String file = specFile == null ? null : specFile.toString();
    for (CategoryDef category : spec.getCategories().values()) {
      if (category.getHttpStatus() == null) {
        String path = "categories." + category.getName() + ".httpStatus";
        org.yaml.snakeyaml.error.Mark mark = marks.get(path);
        Integer line = mark == null ? null : mark.getLine() + 1;
        Integer column = mark == null ? null : mark.getColumn() + 1;
        diagnostics.add(new Diagnostic(DiagnosticSeverity.ERROR,
            "httpStatus is required when Spring handler generation is enabled",
            path, file, line, column));
      }
    }
    return diagnostics;
  }

  private Path writeDocs(EdlSpec spec, Path outputDirectory) throws IOException {
    Path docsFile = outputDirectory.resolve("edl-docs.md");
    String content = "# \uD83D\uDCD6 EDL Exceptions\n\n"
        + "## \uD83E\uDDF1 Package\n\n"
        + "`" + spec.getPackageName() + "`\n\n"
        + "## \uD83E\uDDF1 Base Exception\n\n"
        + "`" + spec.getBaseException() + "Exception`\n\n"
        + "## \uD83E\uDDF1 Source\n\n"
        + "`" + spec.getSource() + "`\n\n"
        + "## \uD83E\uDDF1 Categories\n\n"
        + "Generated " + spec.getCategories().size() + " category exceptions.\n\n"
        + "## \uD83D\uDCDD Errors\n\n"
        + "Generated " + spec.getErrors().size() + " concrete exceptions.\n";
    if (Files.exists(docsFile)) {
      String existing = Files.readString(docsFile, StandardCharsets.UTF_8);
      if (existing.equals(content)) {
        return docsFile;
      }
    }
    Files.createDirectories(outputDirectory);
    Files.writeString(docsFile, content, StandardCharsets.UTF_8);
    return docsFile;
  }
}
