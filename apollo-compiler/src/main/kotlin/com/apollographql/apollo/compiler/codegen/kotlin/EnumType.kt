package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.AST
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

fun KotlinCodeGen.enumTypeSpec(enumType: AST.EnumType): TypeSpec {
  return with(enumType) {
    TypeSpec.enumBuilder(name)
        .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
        .addAnnotation(generatedByApolloAnnotation)
        .primaryConstructor(primaryConstructorSpec)
        .addProperty(rawValuePropertySpec)
        .apply {
          values.forEach { value -> addEnumConstant(value.constName, value.enumConstTypeSpec) }
          addEnumConstant("UNKNOWN__", unknownEnumConstTypeSpec)
        }
        .addType(companionObjectSpec)
        .build()
  }
}

private val primaryConstructorSpec = FunSpec.constructorBuilder()
    .addParameter("rawValue", String::class)
    .build()

private val rawValuePropertySpec = PropertySpec.builder("rawValue", String::class)
    .initializer("rawValue")
    .build()

private val AST.EnumType.Value.enumConstTypeSpec: TypeSpec
  get() {
    return TypeSpec.anonymousClassBuilder()
        .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
        .applyIf(isDeprecated) { addAnnotation(KotlinCodeGen.deprecatedAnnotation(deprecationReason)) }
        .addSuperclassConstructorParameter("%S", value)
        .build()
  }

private val unknownEnumConstTypeSpec: TypeSpec = TypeSpec.anonymousClassBuilder()
    .addKdoc("%L", "Auto generated constant for unknown enum values\n")
    .addSuperclassConstructorParameter("%S", "UNKNOWN__")
    .build()

private val AST.EnumType.companionObjectSpec: TypeSpec
  get() {
    return TypeSpec.companionObjectBuilder()
        .addFunction(safeValueOfFunSpec)
        .build()
  }

private val AST.EnumType.safeValueOfFunSpec: FunSpec
  get() {
    return FunSpec.builder("safeValueOf")
        .addAnnotation(JvmStatic::class)
        .addParameter("rawValue", String::class)
        .returns(ClassName("", name))
        .addCode("return values().find { it.rawValue == rawValue } ?: UNKNOWN__")
        .build()
  }