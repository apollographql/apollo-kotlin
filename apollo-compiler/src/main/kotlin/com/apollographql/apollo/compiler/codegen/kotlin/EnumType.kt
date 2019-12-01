package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.EnumType
import com.squareup.kotlinpoet.*

internal fun EnumType.typeSpec(generateAsInternal: Boolean = false) =
    TypeSpec
        .enumBuilder(name)
        .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
        .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
        .primaryConstructor(primaryConstructorSpec)
        .addProperty(rawValuePropertySpec)
        .apply {
          values.forEach { value -> addEnumConstant(value.constName, value.enumConstTypeSpec) }
          addEnumConstant("UNKNOWN__", unknownEnumConstTypeSpec)
        }
        .addType(companionObjectSpec)
        .build()

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
        .applyIf(isDeprecated) { addAnnotation(KotlinCodeGen.deprecatedAnnotation(deprecationReason)) }
        .addSuperclassConstructorParameter("%S", value)
        .build()
  }

private val unknownEnumConstTypeSpec: TypeSpec =
    TypeSpec
        .anonymousClassBuilder()
        .addKdoc("%L", "Auto generated constant for unknown enum values\n")
        .addSuperclassConstructorParameter("%S", "UNKNOWN__")
        .build()

private val EnumType.companionObjectSpec: TypeSpec
  get() {
    return TypeSpec
        .companionObjectBuilder()
        .addFunction(safeValueOfFunSpec)
        .build()
  }

private val EnumType.safeValueOfFunSpec: FunSpec
  get() {
    return FunSpec
        .builder("safeValueOf")
        .addParameter("rawValue", String::class)
        .returns(ClassName("", name))
        .addStatement("return values().find·{·it.rawValue·==·rawValue·} ?: UNKNOWN__")
        .build()
  }
