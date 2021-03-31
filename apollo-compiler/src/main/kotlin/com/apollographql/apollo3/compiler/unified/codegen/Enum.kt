package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.fromResponse
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.reader
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.responseAdapterCache
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.value
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.backend.codegen.deprecatedAnnotation
import com.apollographql.apollo3.compiler.backend.codegen.toResponseFunSpecBuilder
import com.apollographql.apollo3.compiler.unified.CodegenLayout
import com.apollographql.apollo3.compiler.unified.IrEnum
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDescription
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode


internal fun IrEnum.apolloFileSpecs(
    layout: CodegenLayout,
    enumAsSealedClassPatternFilters: Set<String>,
): List<ApolloFileSpec> {
  val regexes = enumAsSealedClassPatternFilters.map { Regex(it) }
  val asSealedClass = enumAsSealedClassPatternFilters.isNotEmpty() && regexes.any { pattern ->
    name.matches(pattern)
  }

  val enumTypeSpec =   if (asSealedClass)
    toSealedClassTypeSpec(layout)
  else
    toEnumTypeSpec(layout)

  return listOf(
      ApolloFileSpec(
          packageName = layout.enumClassName(name).packageName,
          typeSpec = enumTypeSpec
      ),
      ApolloFileSpec(
          packageName = layout.enumAdapterClassName(name).packageName,
          typeSpec = adapterTypeSpec(layout, asSealedClass)
      )
  )
}

private fun IrEnum.toEnumTypeSpec(layout: CodegenLayout): TypeSpec {
  return TypeSpec
      .enumBuilder(layout.enumName(name))
      .applyIf(description?.isNotBlank() == true) { addKdoc("%L\n", description!!) }
      .primaryConstructor(primaryConstructorWithOverriddenParamSpec)
      .addProperty(rawValuePropertySpec)
      .apply {
        values.forEach { value -> addEnumConstant(layout.enumValueName(value.name), value.typeSpec()) }
        addEnumConstant("UNKNOWN__", unknownEnumConstTypeSpec)
      }
      .build()
}

private fun IrEnum.adapterTypeSpec(layout: CodegenLayout, asSealedClass: Boolean): TypeSpec {
  val adaptedTypeName = layout.enumClassName(name)
  val fromResponseFunSpec = FunSpec.builder(fromResponse)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(reader, JsonReader::class)
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .returns(adaptedTypeName)
      .addCode(
          CodeBlock.builder()
              .addStatement("val rawValue = reader.nextString()!!")
              .beginControlFlow("return when(rawValue)")
              .add(
                  values
                      .map { CodeBlock.of("%S -> %L.%L", it.name, layout.enumName(name), layout.enumValueName(it.name)) }
                      .joinToCode(separator = "\n", suffix = "\n")
              )
              .add("else -> %L.UNKNOWN__%L\n", layout.enumName(name), if (asSealedClass) "(rawValue)" else "")
              .endControlFlow()
              .build()
      )
      .addModifiers(KModifier.OVERRIDE)
      .build()
  val toResponseFunSpec = toResponseFunSpecBuilder(adaptedTypeName)
      .addCode("$writer.$value($value.rawValue)")
      .build()

  return TypeSpec
      .objectBuilder(layout.enumResponseAdapterName(name))
      .addSuperinterface(ResponseAdapter::class.asClassName().parameterizedBy(adaptedTypeName))
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
        .addParameter("rawValue", String::class)
        .build()

private val rawValuePropertySpec =
    PropertySpec
        .builder("rawValue", String::class)
        .initializer("rawValue")
        .build()

private fun IrEnum.Value.typeSpec(): TypeSpec {
  return TypeSpec
      .anonymousClassBuilder()
      .applyIf(description?.isNotBlank() == true) { addKdoc("%L\n", description!!) }
      .applyIf(deprecationReason != null) { addAnnotation(deprecatedAnnotation(deprecationReason!!)) }
      .addSuperclassConstructorParameter("%S", name)
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

private fun IrEnum.toSealedClassTypeSpec(layout: CodegenLayout): TypeSpec {
  return TypeSpec
      .classBuilder(layout.enumName(name))
      .maybeAddDescription(description)
      .addModifiers(KModifier.SEALED)
      .primaryConstructor(primaryConstructorWithOverriddenParamSpec)
      .addProperty(rawValuePropertySpec)
      .addTypes(values.map { value ->
        value.toObjectTypeSpec(layout, ClassName("", layout.enumName(name)))
      })
      .addType(unknownValueTypeSpec(layout))
      .build()
}

private fun IrEnum.Value.toObjectTypeSpec(layout: CodegenLayout, superClass: TypeName): TypeSpec {
  return TypeSpec.objectBuilder(layout.enumValueName(name))
      .applyIf(description?.isNotBlank() == true) { addKdoc("%L\n", description!!) }
      .applyIf(deprecationReason != null) { addAnnotation(deprecatedAnnotation(deprecationReason!!)) }
      .superclass(superClass)
      .addSuperclassConstructorParameter("rawValue = %S", name)
      .build()
}

private fun IrEnum.unknownValueTypeSpec(layout: CodegenLayout): TypeSpec {
  return TypeSpec.classBuilder("UNKNOWN__")
      .addKdoc("%L", "Auto generated constant for unknown enum values\n")
      .primaryConstructor(primaryConstructorSpec)
      .superclass(ClassName("", layout.enumName(name)))
      .addSuperclassConstructorParameter("rawValue = rawValue")
      .build()
}
