package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.EnumType
import com.squareup.kotlinpoet.*

internal fun EnumType.typeSpec(
    generateAsInternal: Boolean = false,
    enumAsSealedClassPatternFilters: List<Regex>
): TypeSpec {
  val asSealedClass = enumAsSealedClassPatternFilters.isNotEmpty() && enumAsSealedClassPatternFilters.any { pattern ->
    name.matches(pattern)
  }

  return if (asSealedClass) toSealedClassTypeSpec(generateAsInternal)
  else toEnumTypeSpec(generateAsInternal)
}

private fun EnumType.toEnumTypeSpec(generateAsInternal: Boolean): TypeSpec {
  return TypeSpec
      .enumBuilder(name)
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .primaryConstructor(primaryConstructorSpec)
      .addProperty(rawValuePropertySpec)
      .apply {
        values.forEach { value -> addEnumConstant(value.constName, value.enumConstTypeSpec) }
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

private val rawValuePropertySpec =
    PropertySpec
        .builder("rawValue", String::class)
        .initializer("rawValue")
        .build()

private val EnumType.Value.enumConstTypeSpec: TypeSpec
  get() {
    return TypeSpec
        .anonymousClassBuilder()
        .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
        .applyIf(deprecationReason != null) { addAnnotation(KotlinCodeGen.deprecatedAnnotation(deprecationReason!!)) }
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

private val EnumType.enumCompanionObjectSpec: TypeSpec
  get() {
    return TypeSpec
        .companionObjectBuilder()
        .addFunction(enumSafeValueOfFunSpec)
        .build()
  }

private val EnumType.enumSafeValueOfFunSpec: FunSpec
  get() {
    return FunSpec
        .builder("safeValueOf")
        .addParameter("rawValue", String::class)
        .returns(ClassName("", name))
        .addStatement("return values().find·{·it.rawValue·==·rawValue·} ?: UNKNOWN__")
        .build()
  }

private fun EnumType.toSealedClassTypeSpec(generateAsInternal: Boolean): TypeSpec {
  return TypeSpec
      .classBuilder(name)
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addModifiers(KModifier.SEALED)
      .primaryConstructor(primaryConstructorSpec)
      .addProperty(rawValuePropertySpec)
      .addTypes(values.map { value -> value.toObjectTypeSpec(ClassName("", name)) })
      .addType(unknownValueTypeSpec)
      .addType(sealedClassCompanionObjectSpec)
      .build()
}

private fun EnumType.Value.toObjectTypeSpec(superClass: TypeName): TypeSpec {
  return TypeSpec.objectBuilder(constName)
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .applyIf(deprecationReason != null) { addAnnotation(KotlinCodeGen.deprecatedAnnotation(deprecationReason!!)) }
      .superclass(superClass)
      .addSuperclassConstructorParameter("rawValue = %S", value)
      .build()
}

private val EnumType.unknownValueTypeSpec: TypeSpec
  get() {
    return TypeSpec.classBuilder("UNKNOWN__")
        .addKdoc("%L", "Auto generated constant for unknown enum values\n")
        .primaryConstructor(primaryConstructorSpec)
        .superclass(ClassName("", name))
        .addSuperclassConstructorParameter("rawValue = rawValue")
        .build()
  }

private val EnumType.sealedClassCompanionObjectSpec: TypeSpec
  get() {
    return TypeSpec
        .companionObjectBuilder()
        .addFunction(sealedClassSafeValueOfFunSpec)
        .build()
  }

private val EnumType.sealedClassSafeValueOfFunSpec: FunSpec
  get() {
    val returnClassName = ClassName("", name)
    return FunSpec
        .builder("safeValueOf")
        .addParameter("rawValue", String::class)
        .returns(returnClassName)
        .beginControlFlow("return when(rawValue)")
        .addCode(
            values
                .map { CodeBlock.of("%S -> %L", it.value, it.constName) }
                .joinToCode(separator = "\n", suffix = "\n")
        )
        .addCode("else -> UNKNOWN__(rawValue)\n")
        .endControlFlow()
        .build()
  }
