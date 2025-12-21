# 📦 EDL Compiler and Maven Plugin

## 📚 Overview
EDL turns a YAML Exception Definition Language spec into a clean Java exception hierarchy. It generates a root exception, category base classes, and concrete errors with stable formatting, strong typing, and deterministic output. Codes are composed as `<prefix><numericCode>` without a dash.

📦 Modules:
- `edl-core` for parsing, validation, and Java generation
- `edl-maven-plugin` for Maven integration
- `edl-samples` for golden file tests and examples

## ✅ Defaults
- Target language is Java
- YAML is the only input format for v1

## 🧾 YAML Spec Examples
```yaml
package: com.example.hello
rootException: HelloRoot
source: hello-service
categories:
  Common:
    codePrefix: CM
    httpStatus: 500
    params:
      source: String
      code: String
      description: String
      detail: String
errors:
  helloWorld:
    category: Common
    fixed:
      code: 1
      description: "Hello {name}"
      detail: "Hello detail {name}"
    required:
      name: String
response:
  source: source
  code: code
  description: description
  detail: detail
  recoverable: recoverable
  details: details
```

```yaml
package: com.example.hierarchy
rootException: Hierarchy
source: hierarchy-service
categories:
  Base:
    codePrefix: BASE
    httpStatus: 500
    params:
      source: String
      code: String
      description: String
      detail: String
  Validation:
    parent: Base
    codePrefix: VAL
    httpStatus: 400
    params:
      source: String
      code: String
      description: String
      detail: String
errors:
  invalidEmail:
    category: Validation
    fixed:
      code: "12"
      description: "Invalid email {email}"
      detail: "Invalid email {email} detail"
    required:
      email: String
```

## 🧾 Response Mapping
Use `response` to rename fields in the Spring handler response. Keys are the core fields and values are the response field names.

```yaml
response:
  source: source
  code: reasonCode
  description: message
  detail: detail
  recoverable: canRecover
  details: detailsJson
```

## ☕ Generated Java Examples
```java
public abstract class HelloRootException extends RuntimeException {
  private static final String SOURCE = "hello-service";
  private final String code;
  private final String descriptionTemplate;
  private final String detailTemplate;
  private final Map<String, Object> details;

  protected HelloRootException(String code, String descriptionTemplate, String detailTemplate,
      Map<String, Object> details, Throwable cause) {
    super(descriptionTemplate, cause);
    this.code = Objects.requireNonNull(code, "code");
    this.descriptionTemplate = Objects.requireNonNull(descriptionTemplate, "descriptionTemplate");
    this.detailTemplate = Objects.requireNonNull(detailTemplate, "detailTemplate");
    this.details = Map.copyOf(Objects.requireNonNull(details, "details"));
  }

  public String code() {
    return code;
  }

  public String descriptionTemplate() {
    return descriptionTemplate;
  }

  public String detailTemplate() {
    return detailTemplate;
  }

  public String description() {
    return renderTemplate(descriptionTemplate, renderValues());
  }

  public String detail() {
    return renderTemplate(detailTemplate, renderValues());
  }

  public Map<String, Object> details() {
    return details;
  }

  public String source() {
    return SOURCE;
  }

  public boolean recoverable() {
    return false;
  }

  public Map<String, Object> errorInfo() {
    return coreValues();
  }
}
```

```java
public final class HelloWorldException extends CommonException {
  public static final String ERROR_CODE = "0001";
  public static final String DESCRIPTION_TEMPLATE = "Hello {name}";
  public static final String DETAIL_TEMPLATE = "Hello detail {name}";
  public static final boolean RECOVERABLE = false;
  private final String name;

  private HelloWorldException(String name, Map<String, Object> details, Throwable cause) {
    super(ERROR_CODE, DESCRIPTION_TEMPLATE, DETAIL_TEMPLATE, details, cause);
    this.name = name;
  }

  public String name() {
    return name;
  }

  public boolean recoverable() {
    return RECOVERABLE;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String name;
    private Throwable cause;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    public HelloWorldException build() {
      String resolvedName = this.name;
      if (resolvedName == null) {
        throw new IllegalStateException("Missing required param: name");
      }
      Map<String, Object> details = new LinkedHashMap<>();
      details.put("name", resolvedName);
      return new HelloWorldException(resolvedName, details, cause);
    }
  }
}
```

## 🔌 Maven Plugin Configuration
```xml
<plugin>
  <groupId>com.edl</groupId>
  <artifactId>edl-maven-plugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <executions>
    <execution>
      <goals>
        <goal>generate-exceptions</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <specFile>${project.basedir}/src/main/resources/edl.yml</specFile>
    <outputDirectory>${project.build.directory}/generated-sources/edl</outputDirectory>
    <failOnWarnings>false</failOnWarnings>
    <generateDocs>false</generateDocs>
    <generateSpringHandler>false</generateSpringHandler>
  </configuration>
</plugin>
```

## 🌱 Spring Handler Generation
Enable the Spring handler to generate a `@RestControllerAdvice` in the same package. The handler catches the root exception and returns a response with `source`, `code`, `description`, `detail`, `recoverable`, and `details` where `details` is a JSON string. It uses Jackson `ObjectMapper`, so include `jackson-databind` at runtime. When this is enabled, every category must define `httpStatus`.

## 📖 Developer Guide
See `DEV_GUIDE.md` for YAML examples, Maven usage, and generated exception usage.

## 🛠️ Troubleshooting
- Ensure the YAML file is valid and uses the required keys
- Check error messages for a YAML key path like `errors.userNotFound.fixed.code`
- Verify category prefixes are unique across the spec
- Confirm every description/detail placeholder has a matching param and vice versa
 - Root exception names must be PascalCase and omit the `Exception` suffix
