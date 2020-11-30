package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.api.EnumValue
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.EnumType.typeSpec(
    generateAsInternal: Boolean = false,
    enumAsSealedClassPatternFilters: List<Regex>
): TypeSpec {
  val asSealedClass = enumAsSealedClassPatternFilters.isNotEmpty() && enumAsSealedClassPatternFilters.any { pattern ->
    name.matches(pattern)
  }

  return if (asSealedClass) toSealedClassTypeSpec(generateAsInternal)
  else toEnumTypeSpec(generateAsInternal)
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
      .addType(enumCompanionObjectSpec)
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

private val CodeGenerationAst.EnumType.enumCompanionObjectSpec: TypeSpec
  get() {
    return TypeSpec
        .companionObjectBuilder()
        .addFunction(enumSafeValueOfFunSpec)
        .build()
  }

private val CodeGenerationAst.EnumType.enumSafeValueOfFunSpec: FunSpec
  get() {
    return FunSpec
        .builder("safeValueOf")
        .addParameter("rawValue", String::class)
        .returns(ClassName("", name.escapeKotlinReservedWord()))
        .addStatement("return values().find·{·it.rawValue·==·rawValue·} ?: UNKNOWN__")
        .build()
  }

private fun CodeGenerationAst.EnumType.toSealedClassTypeSpec(generateAsInternal: Boolean): TypeSpec {
  return TypeSpec
      .classBuilder(name.escapeKotlinReservedWord())
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addModifiers(KModifier.SEALED)
      .primaryConstructor(primaryConstructorWithOverriddenParamSpec)
      .addSuperinterface(EnumValue::class)
      .addProperty(rawValuePropertySpec)
      .addTypes(consts.map { value -> value.toObjectTypeSpec(ClassName("", name.escapeKotlinReservedWord())) })
      .addType(unknownValueTypeSpec)
      .addType(sealedClassCompanionObjectSpec)
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

private val CodeGenerationAst.EnumType.sealedClassCompanionObjectSpec: TypeSpec
  get() {
    return TypeSpec
        .companionObjectBuilder()
        .addFunction(sealedClassSafeValueOfFunSpec)
        .build()
  }

private val CodeGenerationAst.EnumType.sealedClassSafeValueOfFunSpec: FunSpec
  get() {
    val returnClassName = ClassName("", name.escapeKotlinReservedWord())
    return FunSpec
        .builder("safeValueOf")
        .addParameter("rawValue", String::class)
        .returns(returnClassName)
        .beginControlFlow("return when(rawValue)")
        .addCode(
            consts
                .map { CodeBlock.of("%S -> %L", it.value, it.constName.escapeKotlinReservedWord()) }
                .joinToCode(separator = "\n", suffix = "\n")
        )
        .addCode("else -> UNKNOWN__(rawValue)\n")
        .endControlFlow()
        .build()
  }
