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
- `httpStatus` optional int
- `retryable` optional boolean
- `abstract` optional boolean, default true

Error fields:
- `category` required string
- `code` required number or string, normalized to 4 digits
- `message` required string with `{placeholders}`
- `params` required map of `name` to Java type string
- `requiredParams` optional list of param names
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

Recoverable error with extra params:
```yaml
package: com.example.payments
rootException: PaymentsException
source: payments-service
categories:
  Billing:
    codePrefix: BILL
    retryable: true
errors:
  paymentDeclined:
    category: Billing
    code: 5
    message: "Payment declined {paymentId}"
    params:
      paymentId: String
      reason: String
    requiredParams:
      - paymentId
    recoverable: true
response:
  source: source
  code: reasonCode
  description: description
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

The response map contains `source`, `code`, `description`, `recoverable`, and `details` where `details` is a JSON string.

## ☕ Using Generated Exceptions
```java
HelloWorldException exception = HelloWorldException.builder()
    .name("Ada")
    .build();

String code = exception.code();
String template = exception.messageTemplate();
Map<String, Object> details = exception.details();
boolean recoverable = exception.recoverable();

Map<String, Object> info = exception.errorInfo();
```

Notes:
- `errorInfo().description` is the template expanded with `details`.
- `details` contains only the typed params from the builder.
- `recoverable` defaults to `false` unless set in the error.
