package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.api.EnumValue
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.EnumType.typeSpecs(
    generateAsInternal: Boolean = false,
    enumAsSealedClassPatternFilters: List<Regex>,
    packageName: String
): List<TypeSpec> {
  val asSealedClass = enumAsSealedClassPatternFilters.isNotEmpty() && enumAsSealedClassPatternFilters.any { pattern ->
    name.matches(pattern)
  }

  return listOf(
      if (asSealedClass)
        toSealedClassTypeSpec(generateAsInternal)
      else
        toEnumTypeSpec(generateAsInternal),
      adapterTypeSpec(generateAsInternal, asSealedClass, packageName)
  )
}

private fun CodeGenerationAst.EnumType.toEnumTypeSpec(generateAsInternal: Boolean): TypeSpec {
  return TypeSpec
      .enumBuilder(name.escapeKotlinReservedWord())
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .primaryConstructor(primaryConstructorWithOverriddenParamSpec)
      .addSuperinterface(EnumValue::class)
      .addProperty(rawValuePropertySpec)
      .apply {
        consts.forEach { value -> addEnumConstant(value.constName, value.enumConstTypeSpec) }
        addEnumConstant("UNKNOWN__", unknownEnumConstTypeSpec)
      }
      .build()
}

private fun CodeGenerationAst.EnumType.adapterTypeSpec(generateAsInternal: Boolean, asSealedClass: Boolean, packageName: String): TypeSpec {
  val fromResponseFunSpec = FunSpec.builder("fromResponse")
      .addParameter("reader", JsonReader::class)
      .addParameter("__typename", String::class.asTypeName().copy(nullable = true))
      .returns(ClassName(packageName, name.escapeKotlinReservedWord()))
      .addCode(
          CodeBlock.builder()
              .addStatement("val rawValue = reader.nextString()!!")
              .beginControlFlow("return when(rawValue)")
              .add(
                  consts
                      .map { CodeBlock.of("%S -> %L.%L", it.value, name.escapeKotlinReservedWord(), it.constName.escapeKotlinReservedWord()) }
                      .joinToCode(separator = "\n", suffix = "\n")
              )
              .add("else -> %L.UNKNOWN__%L\n", name.escapeKotlinReservedWord(), if (asSealedClass) "(rawValue)" else "")
              .endControlFlow()
              .build()
      )
      .addModifiers(KModifier.OVERRIDE)
      .build()
  val toResponseFunSpec = FunSpec.builder("toResponse")
      .addParameter("writer", JsonWriter::class)
      .addParameter("value", ClassName(packageName, name.escapeKotlinReservedWord()))
      .addCode("writer.value(value.rawValue)")
      .addModifiers(KModifier.OVERRIDE)
      .build()

  return TypeSpec
      .objectBuilder("${name.escapeKotlinReservedWord()}_ResponseAdapter")
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addSuperinterface(ResponseAdapter::class.asClassName().parameterizedBy(ClassName(packageName, name.escapeKotlinReservedWord())))
      .addFunction(fromResponseFunSpec)
      .addFunction(toResponseFunSpec)
      .build()
}

private val primaryConstructorSpec =
    FunSpec
        .constructorBuilder()
        .addParameter("rawValue", String::class)
        .build()

private val primaryConstructorWithOverriddenParamSpec =
    FunSpec
        .constructorBuilder()
        .addParameter("rawValue", String::class, KModifier.OVERRIDE)
        .build()

private val rawValuePropertySpec =
    PropertySpec
        .builder("rawValue", String::class)
        .initializer("rawValue")
        .build()

private val CodeGenerationAst.EnumConst.enumConstTypeSpec: TypeSpec
  get() {
    return TypeSpec
        .anonymousClassBuilder()
        .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
        .applyIf(deprecationReason != null) { addAnnotation(deprecatedAnnotation(deprecationReason!!)) }
        .addSuperclassConstructorParameter("%S", value)
        .build()
  }

private val unknownEnumConstTypeSpec: TypeSpec
  get() {
    return TypeSpec
        .anonymousClassBuilder()
        .addKdoc("%L", "Auto generated constant for unknown enum values\n")
        .addSuperclassConstructorParameter("%S", "UNKNOWN__")
        .build()
  }

private fun CodeGenerationAst.EnumType.toSealedClassTypeSpec(generateAsInternal: Boolean): TypeSpec {
  return TypeSpec
      .classBuilder(kotlinNameForEnum(name))
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addModifiers(KModifier.SEALED)
      .primaryConstructor(primaryConstructorWithOverriddenParamSpec)
      .addSuperinterface(EnumValue::class)
      .addProperty(rawValuePropertySpec)
      .addTypes(consts.map { value -> value.toObjectTypeSpec(ClassName("", kotlinNameForEnum(name))) })
      .addType(unknownValueTypeSpec)
      .build()
}

private fun CodeGenerationAst.EnumConst.toObjectTypeSpec(superClass: TypeName): TypeSpec {
  return TypeSpec.objectBuilder(constName.escapeKotlinReservedWord())
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .applyIf(deprecationReason != null) { addAnnotation(deprecatedAnnotation(deprecationReason!!)) }
      .superclass(superClass)
      .addSuperclassConstructorParameter("rawValue = %S", value)
      .build()
}

private val CodeGenerationAst.EnumType.unknownValueTypeSpec: TypeSpec
  get() {
    return TypeSpec.classBuilder("UNKNOWN__")
        .addKdoc("%L", "Auto generated constant for unknown enum values\n")
        .primaryConstructor(primaryConstructorSpec)
        .superclass(ClassName("", name.escapeKotlinReservedWord()))
        .addSuperclassConstructorParameter("rawValue = rawValue")
        .build()
  }
