# 🧭 Developer Guide

## 📦 Overview
This project compiles EDL YAML into Java exception hierarchies and provides a Maven plugin to wire generation into builds. Codes are composed as `<prefix><numericCode>` without a dash.

## 🧾 YAML Format
Top level keys:
- `package` string
- `baseException` string
- `source` string
- `options` optional map
- `categories` map
- `errors` map

Category fields:
- `parent` optional string
- `codePrefix` required string
- `httpStatus` optional int (required when Spring handler generation is enabled)
- `retryable` optional boolean
- `abstract` optional boolean, default true
- `container` optional boolean, default false (generates a `${Category}ContainerException`)
- `params` optional map of core param names to Java type strings

Base exception name:
- `baseException` should be PascalCase and must not include the `Exception` suffix. The generator appends it automatically.

Error fields:
- `category` required string
- `fixed` required map
  - `code` required number or string, normalized to 4 digits
  - `description` required string with `{placeholders}`
  - `detail` or `details` required string with `{placeholders}`
- `required` optional map of `name` to Java type string
- `optional` optional list of param names (defaults to `String`)
- `recoverable` optional boolean, default false
- `httpStatus` optional int to override the category `httpStatus`
- `response` optional map of core field name to response field name
- `containerResponse` optional template map used for container responses

## 🧪 YAML Examples
Small hello world:
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
      recoverable: boolean
errors:
  helloWorld:
    category: Common
    fixed:
      code: 1
      description: "Hello {name}"
      details: "Hello detail {name}"
    required:
      name: String
```

Recoverable error with extra params:
```yaml
package: com.example.payments
baseException: Payments
source: payments-service
categories:
  Billing:
    codePrefix: BILL
    retryable: true
    httpStatus: 500
    params:
      source: String
      code: String
      description: String
      details: String
errors:
  paymentDeclined:
    category: Billing
    httpStatus: 403
    fixed:
      code: 5
      description: "Payment declined {paymentId}"
      details: "Payment declined {paymentId} detail"
    required:
      paymentId: String
      reason: String
    recoverable: true
response:
  source: Source
  code: ReasonCode
  description: Description
  details: Details
  recoverable: Recoverable
```

Container response without an Errors wrapper:
```yaml
package: com.example.catalog
baseException: Catalog
source: catalog-service
categories:
  Auth:
    codePrefix: AUTH
    httpStatus: 401
    container: true
    params:
      source: String
      code: String
      description: String
      details: String
errors:
  fooError:
    category: Auth
    fixed:
      code: 1
      description: "Userid invalid {userId}"
      details: "Userid {userId} does not exist"
    required:
      userId: String
    optional:
      - region
response:
  source: Source
  code: ReasonCode
  description: Description
  details: Details
  recoverable: Recoverable
containerResponse:
  Error:
    - Source: source
      ReasonCode: code
      Description: description
      Details: details
      Recoverable: recoverable
```

## 🔌 Maven Plugin Usage
```xml
<plugin>
  <groupId>com.edl</groupId>
  <artifactId>edl-maven-plugin</artifactId>
  <version>0.1.1-SNAPSHOT</version>
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
The `deployment/` folder includes helper scripts for Maven deploy workflows:
- `deployment/deploy` detects snapshot vs release and runs `mvn clean deploy`.
- `deployment/provision-deploy` generates `~/.m2/<config>-deploy.xml` from `deployment/deploy-template.xml`.
- `deployment/.excludes` lists modules to skip.

See `deployment/README.md` for full usage.

## 🧩 Spring Handler Generation
Enable the Spring handler to generate a `@RestControllerAdvice` in the same package as the exceptions. The handler catches the base exception type and returns the `httpStatus` configured on the category (or the error override if provided). When Spring handler generation is enabled, base classes include an `httpStatus` field that is passed through constructors. When handler generation is enabled, every category must define `httpStatus`.

```xml
<configuration>
  <generateSpringHandler>true</generateSpringHandler>
</configuration>
```

The response map is built from your `response` mapping (for example `source`, `code`, `description`, `details`, `recoverable`) where `details` is the rendered details message. When handler generation is enabled, every category must define `httpStatus`.

If a category sets `container: true`, a `${Category}ContainerException` is generated. It is a sibling of the category base exception and can hold a list of that category’s exceptions via `add` and `addAll`. The Spring handler renders it using the `containerResponse` template; any string values are treated as keys looked up from `errorInfo`.

## ☕ Using Generated Exceptions
```java
HelloWorldException exception = HelloWorldException.builder()
    .name("Ada")
    .build();

HelloWorldException.builder()
    .name("Ada")
    .throwException();

String code = exception.code();
String template = exception.descriptionTemplate();
String detailTemplate = exception.detailTemplate();
Map<String, Object> details = exception.details();
boolean recoverable = exception.recoverable();

Map<String, Object> info = exception.errorInfo();
```

Notes:
- `errorInfo().description` and `errorInfo().detail` are the templates expanded with params from the builder.
- `details` contains only the typed params from the builder.
- `recoverable` defaults to `false` unless set in the error.
