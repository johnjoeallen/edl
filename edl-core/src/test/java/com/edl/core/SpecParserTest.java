package com.edl.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SpecParserTest {
  @Test
  void parsesAndNormalizesCode() throws Exception {
    String yaml = "package: com.example\n"
        + "rootException: RootEdlException\n"
        + "source: sample-service\n"
        + "categories:\n"
        + "  Validation:\n"
        + "    codePrefix: VAL\n"
        + "errors:\n"
        + "  invalidInput:\n"
        + "    category: Validation\n"
        + "    code: 7\n"
        + "    message: \"Bad {field}\"\n"
        + "    params:\n"
        + "      field: String\n";

    Path temp = Files.createTempFile("edl", ".yml");
    Files.writeString(temp, yaml);
    YamlLoader loader = new YamlLoader();
    YamlDocument document = loader.load(temp);

    SpecParser parser = new SpecParser();
    ParseResult result = parser.parse(temp, document.getData(), document.getMarks());
    assertFalse(result.getDiagnostics().stream().anyMatch(d -> d.getSeverity() == DiagnosticSeverity.ERROR));
    EdlSpec spec = result.getSpec();
    assertNotNull(spec);
    ErrorDef error = spec.getErrors().get("invalidInput");
    assertEquals("0007", error.getNumericCode());
  }

  @Test
  void detectsUnknownCategory() throws Exception {
    String yaml = "package: com.example\n"
        + "rootException: RootEdlException\n"
        + "source: sample-service\n"
        + "categories:\n"
        + "  Validation:\n"
        + "    codePrefix: VAL\n"
        + "errors:\n"
        + "  missing:\n"
        + "    category: NotReal\n"
        + "    code: 1\n"
        + "    message: \"Missing {id}\"\n"
        + "    params:\n"
        + "      id: String\n";

    Path temp = Files.createTempFile("edl", ".yml");
    Files.writeString(temp, yaml);

    YamlLoader loader = new YamlLoader();
    YamlDocument document = loader.load(temp);
    SpecParser parser = new SpecParser();
    ParseResult result = parser.parse(temp, document.getData(), document.getMarks());
    Validator validator = new Validator();
    ValidationResult validation = validator.validate(result.getSpec(), document.getMarks(), temp);

    assertTrue(validation.hasErrors());
  }

  @Test
  void detectsMissingMessageParam() throws Exception {
    String yaml = "package: com.example\n"
        + "rootException: RootEdlException\n"
        + "source: sample-service\n"
        + "categories:\n"
        + "  Validation:\n"
        + "    codePrefix: VAL\n"
        + "errors:\n"
        + "  invalidInput:\n"
        + "    category: Validation\n"
        + "    code: 1\n"
        + "    message: \"Bad {field} {missing}\"\n"
        + "    params:\n"
        + "      field: String\n"
        + "      extra: String\n";

    Path temp = Files.createTempFile("edl", ".yml");
    Files.writeString(temp, yaml);

    YamlLoader loader = new YamlLoader();
    YamlDocument document = loader.load(temp);
    SpecParser parser = new SpecParser();
    ParseResult result = parser.parse(temp, document.getData(), document.getMarks());
    Validator validator = new Validator();
    ValidationResult validation = validator.validate(result.getSpec(), document.getMarks(), temp);

    List<Diagnostic> diagnostics = validation.getDiagnostics();
    assertTrue(diagnostics.stream().anyMatch(d -> d.getMessage().contains("missing a param")));
  }
}
