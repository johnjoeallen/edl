# 🔌 EDL Maven Plugin

## 🧭 Goal
`generate-exceptions` binds to the `generate-sources` phase and generates Java exceptions from an EDL YAML spec.

## ⚙️ Configuration
```xml
<plugin>
  <groupId>com.edl</groupId>
  <artifactId>edl-maven-plugin</artifactId>
  <version>0.1.2</version>
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

## 🧰 Parameters
- `specFile` path to the YAML spec
- `outputDirectory` destination for generated sources
- `failOnWarnings` fail the build when warnings are present
- `generateDocs` emit a simple markdown summary
- `generateSpringHandler` emit a Spring `@RestControllerAdvice` handler
