package com.edl.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.Modifier;

public final class JavaGenerator {
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Za-z][A-Za-z0-9_]*)\\}");
  private static final List<String> DEFAULT_CORE_PARAMS =
      List.of("source", "code", "description", "detail", "details", "recoverable");
  private static final Set<String> DERIVED_PARAMS =
      Set.of("source", "code", "description", "detail", "details", "recoverable");
  private static final Set<String> RENDERABLE_DERIVED_PARAMS =
      Set.of("source", "code", "recoverable");

  public List<Path> generate(EdlSpec spec, Path outputDirectory) throws IOException {
    List<Path> generatedFiles = new ArrayList<>();
    Path packageDir = outputDirectory.resolve(spec.getPackageName().replace('.', '/'));
    Files.createDirectories(packageDir);

    JavaFile rootFile = JavaFile.builder(spec.getPackageName(), buildRootException(spec)).indent("  ").build();
    generatedFiles.add(writeIfChanged(packageDir, rootFile));

    Map<String, ClassName> categoryTypes = new LinkedHashMap<>();
    for (CategoryDef category : spec.getCategories().values()) {
      String className = category.getName() + "Exception";
      categoryTypes.put(category.getName(), ClassName.get(spec.getPackageName(), className));
    }

    for (CategoryDef category : spec.getCategories().values()) {
      JavaFile categoryFile = JavaFile.builder(spec.getPackageName(),
          buildCategoryException(spec, category, categoryTypes)).indent("  ").build();
      generatedFiles.add(writeIfChanged(packageDir, categoryFile));
    }

    for (ErrorDef error : spec.getErrors().values()) {
      CategoryDef category = spec.getCategories().get(error.getCategory());
      JavaFile errorFile = JavaFile.builder(spec.getPackageName(),
          buildErrorException(spec, category, error, categoryTypes)).indent("  ").build();
      generatedFiles.add(writeIfChanged(packageDir, errorFile));
    }

    return generatedFiles;
  }

  public Path generateSpringHandler(EdlSpec spec, Path outputDirectory) throws IOException {
    Path packageDir = outputDirectory.resolve(spec.getPackageName().replace('.', '/'));
    Files.createDirectories(packageDir);
    JavaFile handlerFile = JavaFile.builder(spec.getPackageName(), buildSpringHandler(spec)).indent("  ").build();
    return writeIfChanged(packageDir, handlerFile);
  }

  private TypeSpec buildRootException(EdlSpec spec) {
    ClassName mapType = ClassName.get(Map.class);
    TypeName mapStringObject = ParameterizedTypeName.get(mapType, ClassName.get(String.class), ClassName.get(Object.class));

    FieldSpec codeField = FieldSpec.builder(String.class, "code", Modifier.PRIVATE, Modifier.FINAL).build();
    FieldSpec descriptionTemplateField = FieldSpec.builder(String.class, "descriptionTemplate", Modifier.PRIVATE, Modifier.FINAL).build();
    FieldSpec detailTemplateField = FieldSpec.builder(String.class, "detailTemplate", Modifier.PRIVATE, Modifier.FINAL).build();
    FieldSpec detailsField = FieldSpec.builder(mapStringObject, "details", Modifier.PRIVATE, Modifier.FINAL).build();
    FieldSpec sourceField = FieldSpec.builder(String.class, "SOURCE", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .initializer("$S", spec.getSource())
        .build();

    MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PROTECTED)
        .addParameter(String.class, "code")
        .addParameter(String.class, "descriptionTemplate")
        .addParameter(String.class, "detailTemplate")
        .addParameter(mapStringObject, "details")
        .addParameter(Throwable.class, "cause")
        .addStatement("super(descriptionTemplate, cause)")
        .addStatement("this.code = $T.requireNonNull(code, $S)", Objects.class, "code")
        .addStatement("this.descriptionTemplate = $T.requireNonNull(descriptionTemplate, $S)", Objects.class, "descriptionTemplate")
        .addStatement("this.detailTemplate = $T.requireNonNull(detailTemplate, $S)", Objects.class, "detailTemplate")
        .addStatement("this.details = $T.copyOf($T.requireNonNull(details, $S))", Map.class, Objects.class, "details")
        .build();

    MethodSpec getCode = MethodSpec.methodBuilder("code")
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return code")
        .build();
    MethodSpec getDescriptionTemplate = MethodSpec.methodBuilder("descriptionTemplate")
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return descriptionTemplate")
        .build();
    MethodSpec getDescription = MethodSpec.methodBuilder("description")
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return renderTemplate(descriptionTemplate, renderValues())")
        .build();
    MethodSpec getDetailTemplate = MethodSpec.methodBuilder("detailTemplate")
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return detailTemplate")
        .build();
    MethodSpec getDetail = MethodSpec.methodBuilder("detail")
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return renderTemplate(detailTemplate, renderValues())")
        .build();
    MethodSpec getParameters = MethodSpec.methodBuilder("details")
        .addModifiers(Modifier.PUBLIC)
        .returns(mapStringObject)
        .addStatement("return details")
        .build();
    MethodSpec getSource = MethodSpec.methodBuilder("source")
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return SOURCE")
        .build();
    MethodSpec getErrorInfo = MethodSpec.methodBuilder("errorInfo")
        .addModifiers(Modifier.PUBLIC)
        .returns(mapStringObject)
        .addStatement("return coreValues()")
        .build();
    MethodSpec recoverable = MethodSpec.methodBuilder("recoverable")
        .addModifiers(Modifier.PUBLIC)
        .returns(boolean.class)
        .addStatement("return false")
        .build();

    MethodSpec coreValues = MethodSpec.methodBuilder("coreValues")
        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
        .returns(mapStringObject)
        .build();

    MethodSpec renderValues = MethodSpec.methodBuilder("renderValues")
        .addModifiers(Modifier.PRIVATE)
        .returns(mapStringObject)
        .addStatement("$T values = new $T<>(details)", mapStringObject, LinkedHashMap.class)
        .addStatement("values.put($S, SOURCE)", "source")
        .addStatement("values.put($S, code)", "code")
        .addStatement("values.put($S, recoverable())", "recoverable")
        .addStatement("return values")
        .build();

    MethodSpec renderTemplate = MethodSpec.methodBuilder("renderTemplate")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .returns(String.class)
        .addParameter(String.class, "template")
        .addParameter(mapStringObject, "values")
        .addStatement("$T resolved = template", String.class)
        .beginControlFlow("for ($T.Entry<String, Object> entry : values.entrySet())", Map.class)
        .addStatement("resolved = resolved.replace($S + entry.getKey() + $S, $T.valueOf(entry.getValue()))",
            "{", "}", String.class)
        .endControlFlow()
        .addStatement("return resolved")
        .build();

    return TypeSpec.classBuilder(baseExceptionName(spec))
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .superclass(RuntimeException.class)
        .addField(sourceField)
        .addField(codeField)
        .addField(descriptionTemplateField)
        .addField(detailTemplateField)
        .addField(detailsField)
        .addMethod(constructor)
        .addMethod(getCode)
        .addMethod(getDescriptionTemplate)
        .addMethod(getDescription)
        .addMethod(getDetailTemplate)
        .addMethod(getDetail)
        .addMethod(getParameters)
        .addMethod(getSource)
        .addMethod(getErrorInfo)
        .addMethod(recoverable)
        .addMethod(coreValues)
        .addMethod(renderValues)
        .addMethod(renderTemplate)
        .build();
  }

  private TypeSpec buildCategoryException(EdlSpec spec,
                                          CategoryDef category,
                                          Map<String, ClassName> categoryTypes) {
    ClassName rootClass = ClassName.get(spec.getPackageName(), baseExceptionName(spec));
    ClassName mapType = ClassName.get(Map.class);
    TypeName mapStringObject = ParameterizedTypeName.get(mapType, ClassName.get(String.class), ClassName.get(Object.class));

    TypeName superclass = rootClass;
    if (category.getParent() != null) {
      superclass = categoryTypes.get(category.getParent());
    }

    TypeSpec.Builder type = TypeSpec.classBuilder(category.getName() + "Exception")
        .addModifiers(Modifier.PUBLIC)
        .superclass(superclass);

    if (category.isAbstract()) {
      type.addModifiers(Modifier.ABSTRACT);
    }

    type.addField(FieldSpec.builder(String.class, "CODE_PREFIX", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .initializer("$S", category.getCodePrefix())
        .build());

    List<Map.Entry<String, String>> coreParams = new ArrayList<>(category.getParams().entrySet());
    List<Map.Entry<String, String>> customCoreParams = new ArrayList<>();
    for (Map.Entry<String, String> entry : coreParams) {
      if (!DERIVED_PARAMS.contains(entry.getKey())) {
        customCoreParams.add(entry);
      }
    }

    for (Map.Entry<String, String> entry : customCoreParams) {
      TypeName typeName = parseTypeName(entry.getValue());
      type.addField(FieldSpec.builder(typeName, entry.getKey(), Modifier.PRIVATE, Modifier.FINAL).build());
      type.addMethod(MethodSpec.methodBuilder(entry.getKey())
          .addModifiers(Modifier.PUBLIC)
          .returns(typeName)
          .addStatement("return $L", entry.getKey())
          .build());
    }

    MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PROTECTED)
        .addParameter(String.class, "errorCode")
        .addParameter(String.class, "descriptionTemplate")
        .addParameter(String.class, "detailTemplate")
        .addParameter(mapStringObject, "details")
        .addParameter(Throwable.class, "cause")
        .addStatement("super(CODE_PREFIX + $T.requireNonNull(errorCode, $S), descriptionTemplate, detailTemplate, details, cause)",
            Objects.class, "errorCode")
        .build();
    MethodSpec.Builder constructorBuilder = constructor.toBuilder();
    for (Map.Entry<String, String> entry : customCoreParams) {
      TypeName typeName = parseTypeName(entry.getValue());
      constructorBuilder.addParameter(typeName, entry.getKey());
      constructorBuilder.addStatement("this.$L = $L", entry.getKey(), entry.getKey());
    }
    constructor = constructorBuilder.build();
    type.addMethod(constructor);

    if (category.getHttpStatus() != null) {
      type.addMethod(MethodSpec.methodBuilder("httpStatus")
          .addModifiers(Modifier.PUBLIC)
          .returns(int.class)
          .addStatement("return $L", category.getHttpStatus())
          .build());
    }
    if (category.getRetryable() != null) {
      type.addMethod(MethodSpec.methodBuilder("retryable")
          .addModifiers(Modifier.PUBLIC)
          .returns(boolean.class)
          .addStatement("return $L", category.getRetryable())
          .build());
    }

    List<String> coreParamNames = category.getParams().isEmpty()
        ? DEFAULT_CORE_PARAMS
        : new ArrayList<>(category.getParams().keySet());
    MethodSpec.Builder coreValues = MethodSpec.methodBuilder("coreValues")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .returns(mapStringObject)
        .addStatement("$T values = new $T<>()", mapStringObject, LinkedHashMap.class);
    for (String name : coreParamNames) {
      if ("source".equals(name)) {
        coreValues.addStatement("values.put($S, source())", name);
      } else if ("code".equals(name)) {
        coreValues.addStatement("values.put($S, code())", name);
      } else if ("description".equals(name)) {
        coreValues.addStatement("values.put($S, description())", name);
      } else if ("detail".equals(name)) {
        coreValues.addStatement("values.put($S, detail())", name);
      } else if ("recoverable".equals(name)) {
        coreValues.addStatement("values.put($S, recoverable())", name);
      } else if ("details".equals(name)) {
        coreValues.addStatement("values.put($S, details())", name);
      } else {
        coreValues.addStatement("values.put($S, $L)", name, name);
      }
    }
    coreValues.addStatement("return $T.copyOf(values)", Map.class);
    type.addMethod(coreValues.build());

    return type.build();
  }

  private TypeSpec buildErrorException(EdlSpec spec,
                                       CategoryDef category,
                                       ErrorDef error,
                                       Map<String, ClassName> categoryTypes) {
    ClassName categoryType = categoryTypes.get(category.getName());
    TypeSpec.Builder type = TypeSpec.classBuilder(NameUtils.toPascalCase(error.getName()) + "Exception")
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .superclass(categoryType);

    type.addField(FieldSpec.builder(String.class, "ERROR_CODE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("$S", error.getNumericCode())
        .build());
    type.addField(FieldSpec.builder(String.class, "DESCRIPTION_TEMPLATE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("$S", error.getDescription())
        .build());
    type.addField(FieldSpec.builder(String.class, "DETAIL_TEMPLATE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("$S", error.getDetail())
        .build());
    type.addField(FieldSpec.builder(boolean.class, "RECOVERABLE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("$L", error.isRecoverable())
        .build());

    List<Map.Entry<String, String>> customCoreParams = new ArrayList<>();
    for (Map.Entry<String, String> entry : category.getParams().entrySet()) {
      if (!DERIVED_PARAMS.contains(entry.getKey())) {
        customCoreParams.add(entry);
      }
    }

    List<ParameterSpec> params = new ArrayList<>();
    List<TypeName> paramTypes = new ArrayList<>();
    List<String> coreParamNames = new ArrayList<>();
    for (Map.Entry<String, String> entry : customCoreParams) {
      TypeName typeName = parseTypeName(entry.getValue());
      paramTypes.add(typeName);
      params.add(ParameterSpec.builder(typeName, entry.getKey()).build());
      coreParamNames.add(entry.getKey());
    }

    for (Map.Entry<String, String> entry : error.getRequiredParams().entrySet()) {
      TypeName typeName = parseTypeName(entry.getValue());
      paramTypes.add(typeName);
      params.add(ParameterSpec.builder(typeName, entry.getKey()).build());
      type.addField(FieldSpec.builder(typeName, entry.getKey(), Modifier.PRIVATE, Modifier.FINAL).build());
      type.addMethod(MethodSpec.methodBuilder(entry.getKey())
          .addModifiers(Modifier.PUBLIC)
          .returns(typeName)
          .addStatement("return $L", entry.getKey())
          .build());
    }

    for (Map.Entry<String, String> entry : error.getOptionalParams().entrySet()) {
      TypeName typeName = parseTypeName(entry.getValue());
      paramTypes.add(typeName);
      params.add(ParameterSpec.builder(typeName, entry.getKey()).build());
      type.addField(FieldSpec.builder(typeName, entry.getKey(), Modifier.PRIVATE, Modifier.FINAL).build());
      type.addMethod(MethodSpec.methodBuilder(entry.getKey())
          .addModifiers(Modifier.PUBLIC)
          .returns(typeName)
          .addStatement("return $L", entry.getKey())
          .build());
    }
    type.addMethod(MethodSpec.methodBuilder("recoverable")
        .addModifiers(Modifier.PUBLIC)
        .returns(boolean.class)
        .addStatement("return RECOVERABLE")
        .build());

    TypeName mapStringObject = ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class), ClassName.get(Object.class));
    MethodSpec.Builder ctor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PRIVATE);
    for (ParameterSpec param : params) {
      ctor.addParameter(param);
    }
    ctor.addParameter(mapStringObject, "details");
    ctor.addParameter(Throwable.class, "cause");
    String coreArgs = String.join(", ", coreParamNames);
    String extra = coreArgs.isEmpty() ? "" : ", " + coreArgs;
    ctor.addStatement("super(ERROR_CODE, DESCRIPTION_TEMPLATE, DETAIL_TEMPLATE, $T.requireNonNull(details, $S), cause$L)",
        Objects.class, "details", extra);
    Set<String> coreParamNameSet = new LinkedHashSet<>(coreParamNames);
    for (ParameterSpec param : params) {
      if (!coreParamNameSet.contains(param.name)) {
        ctor.addStatement("this.$L = $L", param.name, param.name);
      }
    }
    type.addMethod(ctor.build());

    type.addMethod(MethodSpec.methodBuilder("builder")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(ClassName.get("", "Builder"))
        .addStatement("return new Builder()")
        .build());

    type.addType(buildBuilder(category, error, params, paramTypes));

    return type.build();
  }

  private TypeSpec buildBuilder(CategoryDef category,
                                ErrorDef error,
                                List<ParameterSpec> params,
                                List<TypeName> paramTypes) {
    TypeSpec.Builder builder = TypeSpec.classBuilder("Builder")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

    ClassName linkedHashMap = ClassName.get(LinkedHashMap.class);
    TypeName mapStringObject = ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class), ClassName.get(Object.class));

    List<TypeName> boxedTypes = new ArrayList<>();
    for (TypeName typeName : paramTypes) {
      boxedTypes.add(typeName.isPrimitive() ? typeName.box() : typeName);
    }

    for (int i = 0; i < params.size(); i++) {
      builder.addField(FieldSpec.builder(boxedTypes.get(i), params.get(i).name, Modifier.PRIVATE).build());
    }
    builder.addField(FieldSpec.builder(Throwable.class, "cause", Modifier.PRIVATE).build());

    for (int i = 0; i < params.size(); i++) {
      ParameterSpec param = params.get(i);
      builder.addMethod(MethodSpec.methodBuilder(param.name)
          .addModifiers(Modifier.PUBLIC)
          .returns(ClassName.get("", "Builder"))
          .addParameter(param)
          .addStatement("this.$L = $L", param.name, param.name)
          .addStatement("return this")
          .build());
    }

    builder.addMethod(MethodSpec.methodBuilder("cause")
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassName.get("", "Builder"))
        .addParameter(Throwable.class, "cause")
        .addStatement("this.cause = cause")
        .addStatement("return this")
        .build());

    ClassName exceptionType = ClassName.get("", NameUtils.toPascalCase(error.getName()) + "Exception");
    MethodSpec.Builder buildMethod = MethodSpec.methodBuilder("build")
        .addModifiers(Modifier.PUBLIC)
        .returns(exceptionType);

    Set<String> required = new LinkedHashSet<>();
    for (String param : category.getParams().keySet()) {
      if (!DERIVED_PARAMS.contains(param)) {
        required.add(param);
      }
    }
    required.addAll(error.getRequiredParams().keySet());
    required.addAll(extractPlaceholders(error.getDescription(), error.getDetail()));

    for (int i = 0; i < params.size(); i++) {
      ParameterSpec param = params.get(i);
      TypeName paramType = paramTypes.get(i);
      TypeName boxedType = boxedTypes.get(i);
      String localName = "resolved" + NameUtils.toPascalCase(param.name);
      buildMethod.addStatement("$T $L = this.$L", boxedType, localName, param.name);
      boolean isRequired = required.contains(param.name) || paramType.isPrimitive();
      if (isRequired) {
        buildMethod.beginControlFlow("if ($L == null)", localName)
            .addStatement("throw new $T($S + $S)", IllegalStateException.class, "Missing required param: ", param.name)
            .endControlFlow();
      }
    }

    buildMethod.addStatement("$T details = new $T<>()", mapStringObject, linkedHashMap);
    for (int i = 0; i < params.size(); i++) {
      ParameterSpec param = params.get(i);
      String localName = "resolved" + NameUtils.toPascalCase(param.name);
      buildMethod.addStatement("details.put($S, $L)", param.name, localName);
    }

    StringBuilder constructorArgs = new StringBuilder();
    for (int i = 0; i < params.size(); i++) {
      if (i > 0) {
        constructorArgs.append(", ");
      }
      String localName = "resolved" + NameUtils.toPascalCase(params.get(i).name);
      TypeName paramType = paramTypes.get(i);
      if (paramType.isPrimitive()) {
        String unboxed = boxedTypes.get(i).toString();
        if ("java.lang.Integer".equals(unboxed)) {
          constructorArgs.append(localName).append(".intValue()");
        } else if ("java.lang.Long".equals(unboxed)) {
          constructorArgs.append(localName).append(".longValue()");
        } else if ("java.lang.Boolean".equals(unboxed)) {
          constructorArgs.append(localName).append(".booleanValue()");
        } else if ("java.lang.Byte".equals(unboxed)) {
          constructorArgs.append(localName).append(".byteValue()");
        } else if ("java.lang.Short".equals(unboxed)) {
          constructorArgs.append(localName).append(".shortValue()");
        } else if ("java.lang.Float".equals(unboxed)) {
          constructorArgs.append(localName).append(".floatValue()");
        } else if ("java.lang.Double".equals(unboxed)) {
          constructorArgs.append(localName).append(".doubleValue()");
        } else if ("java.lang.Character".equals(unboxed)) {
          constructorArgs.append(localName).append(".charValue()");
        } else {
          constructorArgs.append(localName);
        }
      } else {
        constructorArgs.append(localName);
      }
    }
    if (constructorArgs.length() > 0) {
      constructorArgs.append(", ");
    }
    constructorArgs.append("details, cause");

    buildMethod.addStatement("return new $T($L)", exceptionType, constructorArgs.toString());

    builder.addMethod(buildMethod.build());
    return builder.build();
  }


  private TypeName parseTypeName(String type) {
    String trimmed = type.trim();
    int arrayDepth = 0;
    while (trimmed.endsWith("[]")) {
      arrayDepth += 1;
      trimmed = trimmed.substring(0, trimmed.length() - 2).trim();
    }
    TypeName base = parseNonArrayType(trimmed);
    for (int i = 0; i < arrayDepth; i++) {
      base = com.squareup.javapoet.ArrayTypeName.of(base);
    }
    return base;
  }

  private TypeName parseNonArrayType(String type) {
    switch (type) {
      case "byte":
        return TypeName.BYTE;
      case "short":
        return TypeName.SHORT;
      case "int":
        return TypeName.INT;
      case "long":
        return TypeName.LONG;
      case "float":
        return TypeName.FLOAT;
      case "double":
        return TypeName.DOUBLE;
      case "boolean":
        return TypeName.BOOLEAN;
      case "char":
        return TypeName.CHAR;
      case "void":
        return TypeName.VOID;
      default:
        break;
    }

    int genericStart = type.indexOf('<');
    if (genericStart >= 0 && type.endsWith(">")) {
      String raw = type.substring(0, genericStart).trim();
      String args = type.substring(genericStart + 1, type.length() - 1);
      List<TypeName> typeArgs = parseTypeArguments(args);
      return ParameterizedTypeName.get(ClassName.bestGuess(raw), typeArgs.toArray(new TypeName[0]));
    }
    return ClassName.bestGuess(type);
  }

  private List<TypeName> parseTypeArguments(String args) {
    List<TypeName> results = new ArrayList<>();
    int depth = 0;
    int start = 0;
    for (int i = 0; i < args.length(); i++) {
      char ch = args.charAt(i);
      if (ch == '<') {
        depth += 1;
      } else if (ch == '>') {
        depth -= 1;
      } else if (ch == ',' && depth == 0) {
        results.add(parseTypeArgument(args.substring(start, i).trim()));
        start = i + 1;
      }
    }
    String tail = args.substring(start).trim();
    if (!tail.isEmpty()) {
      results.add(parseTypeArgument(tail));
    }
    return results;
  }

  private TypeName parseTypeArgument(String arg) {
    if ("?".equals(arg)) {
      return WildcardTypeName.subtypeOf(Object.class);
    }
    if (arg.startsWith("? extends ")) {
      return WildcardTypeName.subtypeOf(parseTypeName(arg.substring(10)));
    }
    if (arg.startsWith("? super ")) {
      return WildcardTypeName.supertypeOf(parseTypeName(arg.substring(8)));
    }
    return parseTypeName(arg);
  }

  private Set<String> extractPlaceholders(String... templates) {
    Set<String> placeholders = new LinkedHashSet<>();
    for (String template : templates) {
      Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
      while (matcher.find()) {
        placeholders.add(matcher.group(1));
      }
    }
    return placeholders;
  }

  private Path writeIfChanged(Path packageDir, JavaFile javaFile) throws IOException {
    Path file = packageDir.resolve(javaFile.typeSpec.name + ".java");
    String content = javaFile.toString();
    if (Files.exists(file)) {
      String existing = Files.readString(file, StandardCharsets.UTF_8);
      if (existing.equals(content)) {
        return file;
      }
    }
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file;
  }

  private TypeSpec buildSpringHandler(EdlSpec spec) {
    ClassName restControllerAdvice = ClassName.get("org.springframework.web.bind.annotation", "RestControllerAdvice");
    ClassName exceptionHandler = ClassName.get("org.springframework.web.bind.annotation", "ExceptionHandler");
    ClassName responseEntity = ClassName.get("org.springframework.http", "ResponseEntity");
    ClassName objectMapper = ClassName.get("com.fasterxml.jackson.databind", "ObjectMapper");
    ClassName linkedHashMap = ClassName.get(LinkedHashMap.class);
    ClassName mapType = ClassName.get(Map.class);
    TypeName mapStringObject = ParameterizedTypeName.get(mapType, ClassName.get(String.class), ClassName.get(Object.class));

    TypeSpec.Builder type = TypeSpec.classBuilder(baseExceptionName(spec) + "Handler")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(restControllerAdvice);

    type.addField(FieldSpec.builder(objectMapper, "MAPPER", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .initializer("new $T()", objectMapper)
        .build());

    ClassName rootType = ClassName.get(spec.getPackageName(), baseExceptionName(spec));
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("handle" + baseExceptionName(spec))
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(AnnotationSpec.builder(exceptionHandler)
            .addMember("value", "$T.class", rootType)
            .build())
        .addParameter(rootType, "exception")
        .returns(ParameterizedTypeName.get(responseEntity, mapStringObject))
        .addStatement("$T info = exception.errorInfo()", mapStringObject)
        .addStatement("$T body = new $T<>()", mapStringObject, linkedHashMap);
    for (Map.Entry<String, String> entry : spec.getResponseFields().entrySet()) {
      String key = entry.getKey();
      String responseKey = entry.getValue();
      if ("details".equals(key)) {
        methodBuilder.beginControlFlow("if (info.containsKey($S))", key)
            .addStatement("body.put($S, toJson(($T) info.get($S)))", responseKey, mapStringObject, key)
            .endControlFlow();
      } else {
        methodBuilder.beginControlFlow("if (info.containsKey($S))", key)
            .addStatement("body.put($S, info.get($S))", responseKey, key)
            .endControlFlow();
      }
    }
    methodBuilder.addStatement("return $T.status(500).body(body)", responseEntity);
    type.addMethod(methodBuilder.build());

    type.addMethod(MethodSpec.methodBuilder("toJson")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .returns(String.class)
        .addParameter(mapStringObject, "details")
        .beginControlFlow("try")
        .addStatement("return MAPPER.writeValueAsString(details)")
        .nextControlFlow("catch ($T ex)", Exception.class)
        .addStatement("return $S", "{}")
        .endControlFlow()
        .build());

    return type.build();
  }

  private String baseExceptionName(EdlSpec spec) {
    return spec.getBaseException() + "Exception";
  }
}
