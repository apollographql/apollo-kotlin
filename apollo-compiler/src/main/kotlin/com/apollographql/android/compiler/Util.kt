package com.apollographql.android.compiler

import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

fun TypeName.overrideTypeName(typeNameOverrideMap: Map<String, String>): TypeName {
  if (this is ParameterizedTypeName) {
    val typeArguments = typeArguments.map { it.overrideTypeName(typeNameOverrideMap) }.toTypedArray()
    return ParameterizedTypeName.get(rawType, *typeArguments)
  } else if (this is ClassName) {
    return ClassName.get(packageName(), typeNameOverrideMap[simpleName()] ?: simpleName())
  } else if (this is WildcardTypeName) {
    return WildcardTypeName.subtypeOf(upperBounds[0].overrideTypeName(typeNameOverrideMap))
  } else {
    return this
  }
}

fun FieldSpec.overrideType(typeNameOverrideMap: Map<String, String>): FieldSpec =
    FieldSpec.builder(type.overrideTypeName(typeNameOverrideMap).annotated(type.annotations), name)
        .addModifiers(*modifiers.toTypedArray())
        .addAnnotations(annotations)
        .initializer(initializer)
        .build()

fun MethodSpec.overrideReturnType(typeNameOverrideMap: Map<String, String>): MethodSpec =
    MethodSpec.methodBuilder(name)
        .returns(returnType.overrideTypeName(typeNameOverrideMap).annotated(returnType.annotations))
        .addModifiers(*modifiers.toTypedArray())
        .addCode(code)
        .build()

fun TypeSpec.withValueInitConstructor(nullableValueGenerationType: NullableValueType): TypeSpec {
  return toBuilder()
      .addMethod(MethodSpec.constructorBuilder()
          .addModifiers(Modifier.PUBLIC)
          .addParameters(fieldSpecs
              .filter { !it.modifiers.contains(Modifier.STATIC) }
              .map {
                val paramType = if (it.type.isOptional()) {
                  it.type.unwrapOptionalType()
                } else {
                  it.type
                }
                ParameterSpec.builder(paramType, it.name).build()
              })
          .addCode(fieldSpecs
              .filter { !it.modifiers.contains(Modifier.STATIC) }
              .map {
                if (it.type.isOptional() && nullableValueGenerationType != NullableValueType.ANNOTATED) {
                  val optionalType = if (nullableValueGenerationType == NullableValueType.GUAVA_OPTIONAL)
                    ClassNames.GUAVA_OPTIONAL
                  else
                    ClassNames.OPTIONAL
                  CodeBlock.of("this.\$L = \$T.fromNullable(\$L);\n", it.name, optionalType, it.name)
                } else {
                  CodeBlock.of("this.\$L = \$L;\n", it.name, it.name)
                }
              }
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .build())
      .build()
}

fun String.toJavaType(): ClassName =
    ClassName.get(substringBeforeLast(delimiter = ".", missingDelimiterValue = ""), substringAfterLast("."))

fun TypeSpec.withToStringImplementation(): TypeSpec {
  fun printFieldCode(fieldIndex: Int, fieldName: String) =
      CodeBlock.builder()
          .let { if (fieldIndex > 0) it.add(" + \", \"\n") else it.add("\n") }
          .indent()
          .add("+ \$S + \$L", "$fieldName=", fieldName)
          .unindent()
          .build()

  fun methodCode() =
      CodeBlock.builder()
          .beginControlFlow("if (\$L == null)", Util.MEMOIZED_TO_STRING_VAR)
          .add("\$L = \$S", "\$toString", "$name{")
          .add(fieldSpecs
              .filter { !it.hasModifier(Modifier.STATIC) }
              .filter { it.name != Util.MEMOIZED_HASH_CODE_FLAG_VAR }
              .filter { it.name != Util.MEMOIZED_HASH_CODE_VAR }
              .filter { it.name != Util.MEMOIZED_TO_STRING_VAR }
              .map { it.name }
              .mapIndexed(::printFieldCode)
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .add(CodeBlock.builder()
              .indent()
              .add("\n+ \$S;\n", "}")
              .unindent()
              .build())
          .endControlFlow()
          .addStatement("return \$L", Util.MEMOIZED_TO_STRING_VAR)
          .build()

  return toBuilder()
      .addField(FieldSpec.builder(ClassNames.STRING, Util.MEMOIZED_TO_STRING_VAR, Modifier.PRIVATE, Modifier.VOLATILE)
          .build())
      .addMethod(MethodSpec.methodBuilder("toString")
          .addAnnotation(Override::class.java)
          .addModifiers(Modifier.PUBLIC)
          .returns(java.lang.String::class.java)
          .addCode(methodCode())
          .build())
      .build()
}

fun TypeSpec.withEqualsImplementation(): TypeSpec {
  fun equalsFieldCode(fieldIndex: Int, field: FieldSpec) =
      CodeBlock.builder()
          .let { if (fieldIndex > 0) it.add("\n && ") else it }
          .let {
            if (field.type.isPrimitive) {
              if (field.type == TypeName.DOUBLE) {
                it.add("Double.doubleToLongBits(this.\$L) == Double.doubleToLongBits(that.\$L)",
                    field.name, field.name)
              } else {
                it.add("this.\$L == that.\$L", field.name, field.name)
              }
            } else {
              it.add("((this.\$L == null) ? (that.\$L == null) : this.\$L.equals(that.\$L))", field.name,
                  field.name, field.name, field.name)
            }
          }
          .build()

  fun methodCode(typeJavaClass: ClassName) =
      CodeBlock.builder()
          .beginControlFlow("if (o == this)")
          .addStatement("return true")
          .endControlFlow()
          .beginControlFlow("if (o instanceof \$T)", typeJavaClass)
          .addStatement("\$T that = (\$T) o", typeJavaClass, typeJavaClass)
          .add("return ")
          .add(fieldSpecs
              .filter { !it.hasModifier(Modifier.STATIC) }
              .filter { it.name != Util.MEMOIZED_HASH_CODE_FLAG_VAR }
              .filter { it.name != Util.MEMOIZED_HASH_CODE_VAR }
              .filter { it.name != Util.MEMOIZED_TO_STRING_VAR }
              .mapIndexed(::equalsFieldCode)
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .add(";\n")
          .endControlFlow()
          .addStatement("return false")
          .build()

  return toBuilder()
      .addMethod(MethodSpec.methodBuilder("equals")
          .addAnnotation(Override::class.java)
          .addModifiers(Modifier.PUBLIC)
          .returns(TypeName.BOOLEAN)
          .addParameter(ParameterSpec.builder(TypeName.OBJECT, "o").build())
          .addCode(methodCode(ClassName.get("", name)))
          .build())
      .build()
}

fun TypeSpec.withHashCodeImplementation(): TypeSpec {
  fun hashFieldCode(field: FieldSpec) =
      CodeBlock.builder()
          .addStatement("h *= 1000003")
          .let {
            if (field.type.isPrimitive) {
              if (field.type == TypeName.DOUBLE) {
                it.addStatement("h ^= Double.valueOf(\$L).hashCode()", field.name)
              } else if (field.type == TypeName.BOOLEAN) {
                it.addStatement("h ^= Boolean.valueOf(\$L).hashCode()", field.name)
              } else {
                it.addStatement("h ^= \$L", field.name)
              }
            } else {
              it.addStatement("h ^= (\$L == null) ? 0 : \$L.hashCode()", field.name, field.name)
            }
          }
          .build()

  fun methodCode() =
      CodeBlock.builder()
          .beginControlFlow("if (!\$L)", Util.MEMOIZED_HASH_CODE_FLAG_VAR)
          .addStatement("int h = 1")
          .add(fieldSpecs
              .filter { !it.hasModifier(Modifier.STATIC) }
              .filter { it.name != Util.MEMOIZED_HASH_CODE_FLAG_VAR }
              .filter { it.name != Util.MEMOIZED_HASH_CODE_VAR }
              .filter { it.name != Util.MEMOIZED_TO_STRING_VAR }
              .map(::hashFieldCode)
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .addStatement("\$L = h", Util.MEMOIZED_HASH_CODE_VAR)
          .addStatement("\$L = true", Util.MEMOIZED_HASH_CODE_FLAG_VAR)
          .endControlFlow()
          .addStatement("return \$L", Util.MEMOIZED_HASH_CODE_VAR)
          .build()

  return toBuilder()
      .addField(FieldSpec.builder(TypeName.INT, Util.MEMOIZED_HASH_CODE_VAR, Modifier.PRIVATE, Modifier.VOLATILE)
          .build())
      .addField(FieldSpec.builder(TypeName.BOOLEAN, Util.MEMOIZED_HASH_CODE_FLAG_VAR, Modifier.PRIVATE,
          Modifier.VOLATILE).build())
      .addMethod(MethodSpec.methodBuilder("hashCode")
          .addAnnotation(Override::class.java)
          .addModifiers(Modifier.PUBLIC)
          .returns(TypeName.INT)
          .addCode(methodCode())
          .build())
      .build()
}

fun ClassName.mapperFieldName(): String = "${simpleName().decapitalize()}${Util.FIELD_MAPPER_VAR}"

fun TypeName.isOptional(): Boolean {
  val rawType = (this as? ParameterizedTypeName)?.rawType ?: this
  return rawType == ClassNames.OPTIONAL || rawType == ClassNames.GUAVA_OPTIONAL
}

fun TypeName.unwrapOptionalType(): TypeName {
  return if (isOptional()) {
    (this as ParameterizedTypeName).typeArguments.first().annotated(Annotations.NULLABLE)
  } else {
    this
  }
}

fun TypeSpec.removeNestedTypeSpecs(excludeTypeNames: List<String>): TypeSpec =
    TypeSpec.classBuilder(name)
        .superclass(superclass)
        .addJavadoc(javadoc)
        .addAnnotations(annotations)
        .addModifiers(*modifiers.toTypedArray())
        .addSuperinterfaces(superinterfaces)
        .addFields(fieldSpecs)
        .addTypes(typeSpecs.filter { excludeTypeNames.contains(it.name) })
        .addMethods(methodSpecs)
        .let { if (initializerBlock.isEmpty) it else it.addInitializerBlock(initializerBlock) }
        .let { if (staticBlock.isEmpty) it else it.addStaticBlock(staticBlock) }
        .build()

fun TypeSpec.flatNestedTypeSpecs(excludeTypeNames: List<String>): List<TypeSpec> =
    typeSpecs
        .filter { !excludeTypeNames.contains(it.name) }
        .flatMap { listOf(it.removeNestedTypeSpecs(excludeTypeNames)) + it.flatNestedTypeSpecs(excludeTypeNames) }

fun TypeSpec.flatten(excludeTypeNames: List<String>): TypeSpec {
  val nestedTypeSpecs = flatNestedTypeSpecs(excludeTypeNames)
  return removeNestedTypeSpecs(excludeTypeNames)
      .toBuilder()
      .addTypes(nestedTypeSpecs)
      .build()
}

object Util {
  const val MAPPER_TYPE_NAME: String = "Mapper"
  const val FIELD_MAPPER_VAR: String = "FieldMapper"
  const val MEMOIZED_HASH_CODE_VAR: String = "\$hashCode"
  const val MEMOIZED_HASH_CODE_FLAG_VAR: String = "\$hashCodeMemoized"
  const val MEMOIZED_TO_STRING_VAR: String = "\$toString"
}
