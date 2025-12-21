# 📦 EDL Compiler and Maven Plugin

## 📚 Overview
EDL turns a YAML Exception Definition Language spec into a clean Java exception hierarchy. It generates a base exception, category base classes, and concrete errors with stable formatting, strong typing, and deterministic output. Codes are composed as `<prefix><numericCode>` without a dash.

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
baseException: Hello
source: hello-service
categories:
  Common:
    codePrefix: CM
    httpStatus: 500
    params:
      source: String
      code: String
      description: String
      details: String
errors:
  helloWorld:
    category: Common
    fixed:
      code: 1
      description: "Hello {name}"
      details: "Hello detail {name}"
    required:
      name: String
response:
  source: Source
  code: ReasonCode
  description: Description
  details: Details
  recoverable: Recoverable
```

```yaml
package: com.example.hierarchy
baseException: Hierarchy
source: hierarchy-service
categories:
  Base:
    codePrefix: BASE
    httpStatus: 500
    params:
      source: String
      code: String
      description: String
      details: String
  Validation:
    parent: Base
    codePrefix: VAL
    httpStatus: 400
    params:
      source: String
      code: String
      description: String
      details: String
errors:
  invalidEmail:
    category: Validation
    fixed:
      code: "12"
      description: "Invalid email {email}"
      details: "Invalid email {email} detail"
    required:
      email: String
```

## 🧾 Response Mapping
Use `response` to rename fields in the Spring handler response. Keys are the core fields and values are the response field names.

```yaml
response:
  source: Source
  code: ReasonCode
  description: Description
  details: Details
  recoverable: Recoverable
```

## ☕ Generated Java Examples
```java
public abstract class HelloException extends RuntimeException {
  private static final String SOURCE = "hello-service";
  private final String code;
  private final String descriptionTemplate;
  private final String detailTemplate;
  private final Map<String, Object> details;

  protected HelloException(String code, String descriptionTemplate, String detailTemplate,
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
    <specFile>${project.basedir}/src/main/resources/edl.yaml</specFile>
    <outputDirectory>${project.build.directory}/generated-sources/edl</outputDirectory>
    <failOnWarnings>false</failOnWarnings>
    <generateDocs>false</generateDocs>
    <generateSpringHandler>false</generateSpringHandler>
  </configuration>
</plugin>
```

## 🚀 Deployment Scripts
The `deployment/` folder contains a small, self-contained Maven deploy helper:
- `deployment/deploy` runs snapshot vs release deployments by inspecting your `pom.xml`.
- `deployment/provision-deploy` creates a `~/.m2/<config>-deploy.xml` from `deployment/deploy-template.xml`.
- `deployment/.excludes` lists modules to skip during deploys.

See `deployment/README.md` for full usage and examples.

## 🌱 Spring Handler Generation
Enable the Spring handler to generate a `@RestControllerAdvice` in the same package. The handler catches the base exception and returns the `httpStatus` configured on the category (or the error override if provided), with a response built from your `response` mapping (for example `source`, `code`, `description`, `details`, `recoverable`), where `details` is serialized to JSON. When Spring handler generation is enabled, the generated base classes carry an `httpStatus` field and pass it through constructors. It uses Jackson `ObjectMapper`, so include `jackson-databind` at runtime. When this is enabled, every category must define `httpStatus`.

## 📖 Developer Guide
See `DEV_GUIDE.md` for YAML examples, Maven usage, and generated exception usage.

## 🛠️ Troubleshooting
- Ensure the YAML file is valid and uses the required keys
- Check error messages for a YAML key path like `errors.userNotFound.fixed.code`
- Verify category prefixes are unique across the spec
- Confirm every description/detail placeholder has a matching param and vice versa
- Base exception names must be PascalCase and omit the `Exception` suffix
