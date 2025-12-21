# 📦 EDL Compiler and Maven Plugin

## 📚 Overview
EDL turns a YAML Exception Definition Language spec into a clean Java exception hierarchy. It generates a root exception, category base classes, and concrete errors with stable formatting, strong typing, and deterministic output.

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
rootException: HelloRootException
source: hello-service
categories:
  Common:
    codePrefix: CM
errors:
  helloWorld:
    category: Common
    code: 1
    message: "Hello {name}"
    params:
      name: String
    requiredParams:
      - name
```

```yaml
package: com.example.hierarchy
rootException: HierarchyException
source: hierarchy-service
categories:
  Base:
    codePrefix: BASE
    httpStatus: 500
  Validation:
    parent: Base
    codePrefix: VAL
    httpStatus: 400
errors:
  invalidEmail:
    category: Validation
    code: "12"
    message: "Invalid email {email}"
    params:
      email: String
```

## ☕ Generated Java Examples
```java
public abstract class HelloRootException extends RuntimeException {
  private static final String SOURCE = "hello-service";
  private final String code;
  private final String messageTemplate;
  private final Map<String, Object> details;

  protected HelloRootException(String code, String messageTemplate, Map<String, Object> details,
      Throwable cause) {
    super(messageTemplate, cause);
    this.code = Objects.requireNonNull(code, "code");
    this.messageTemplate = Objects.requireNonNull(messageTemplate, "messageTemplate");
    this.details = Map.copyOf(Objects.requireNonNull(details, "details"));
  }

  public Map<String, Object> errorInfo() {
    return Map.ofEntries(
      Map.entry("source", SOURCE),
      Map.entry("code", code),
      Map.entry("description", renderDescription(messageTemplate, details)),
      Map.entry("details", details),
      Map.entry("recoverable", recoverable())
    );
  }
}
```

```java
public final class HelloWorldException extends CommonException {
  public static final String ERROR_CODE = "0001";
  public static final String MESSAGE_TEMPLATE = "Hello {name}";
  public static final boolean RECOVERABLE = false;
  private final String name;

  private HelloWorldException(String name, Map<String, Object> details, Throwable cause) {
    super(ERROR_CODE, MESSAGE_TEMPLATE, details, cause);
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
  </configuration>
</plugin>
```

## 🛠️ Troubleshooting
- Ensure the YAML file is valid and uses the required keys
- Check error messages for a YAML key path like `errors.userNotFound.code`
- Verify category prefixes are unique across the spec
- Confirm every message placeholder has a matching param and vice versa
