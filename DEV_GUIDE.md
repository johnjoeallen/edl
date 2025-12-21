# 🧭 Developer Guide

## 📦 Overview
This project compiles EDL YAML into Java exception hierarchies and provides a Maven plugin to wire generation into builds. Codes are composed as `<prefix><numericCode>` without a dash.

## 🧾 YAML Format
Top level keys:
- `package` string
- `rootException` string
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
- `params` optional map of core param names to Java type strings

Error fields:
- `category` required string
- `fixed` required map
  - `code` required number or string, normalized to 4 digits
  - `description` required string with `{placeholders}`
  - `detail` required string with `{placeholders}`
- `required` optional map of `name` to Java type string
- `optional` optional list of param names (defaults to `String`)
- `recoverable` optional boolean, default false
- `response` optional map of core field name to response field name

## 🧪 YAML Examples
Small hello world:
```yaml
package: com.example.hello
rootException: HelloRootException
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
```

Recoverable error with extra params:
```yaml
package: com.example.payments
rootException: PaymentsException
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
      detail: String
errors:
  paymentDeclined:
    category: Billing
    fixed:
      code: 5
      description: "Payment declined {paymentId}"
      detail: "Payment declined {paymentId} detail"
    required:
      paymentId: String
      reason: String
    recoverable: true
response:
  source: source
  code: reasonCode
  description: description
  detail: detail
  recoverable: recoverable
  details: detailsJson
```

## 🔌 Maven Plugin Usage
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

## 🧩 Spring Handler Generation
Enable the Spring handler to generate a `@RestControllerAdvice` in the same package as the exceptions. The handler catches the root exception type and returns status 500. It uses Jackson `ObjectMapper`, so include `jackson-databind` at runtime.

```xml
<configuration>
  <generateSpringHandler>true</generateSpringHandler>
</configuration>
```

The response map contains `source`, `code`, `description`, `detail`, `recoverable`, and `details` where `details` is a JSON string. When handler generation is enabled, every category must define `httpStatus`.

## ☕ Using Generated Exceptions
```java
HelloWorldException exception = HelloWorldException.builder()
    .name("Ada")
    .build();

String code = exception.code();
String template = exception.descriptionTemplate();
String detailTemplate = exception.detailTemplate();
Map<String, Object> details = exception.details();
boolean recoverable = exception.recoverable();

Map<String, Object> info = exception.errorInfo();
```

Notes:
- `errorInfo().description` and `errorInfo().detail` are the templates expanded with `details`.
- `details` contains only the typed params from the builder.
- `recoverable` defaults to `false` unless set in the error.
