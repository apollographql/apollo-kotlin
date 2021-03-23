package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.Identifier
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.fromResponse
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.reader
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.responseAdapterCache
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.value
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.backend.codegen.adapterPackageName
import com.apollographql.apollo3.compiler.backend.codegen.deprecatedAnnotation
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForEnum
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForEnumValue
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForResponseAdapter
import com.apollographql.apollo3.compiler.backend.codegen.toResponseFunSpecBuilder
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.apollographql.apollo3.compiler.unified.IrEnum
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDescription
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode

internal fun IrEnum.typeName() = ClassName(
    packageName = packageName,
    kotlinNameForEnum(name)
)

internal fun IrEnum.adapterTypeName() = ClassName(
    packageName = adapterPackageName(packageName),
    kotlinNameForResponseAdapter(name)
)


internal fun IrEnum.qualifiedTypeSpecs(
    enumAsSealedClassPatternFilters: List<Regex>,
): List<QualifiedTypeSpec> {
  val asSealedClass = enumAsSealedClassPatternFilters.isNotEmpty() && enumAsSealedClassPatternFilters.any { pattern ->
    name.matches(pattern)
  }

  val enumTypeSpec =   if (asSealedClass)
    toSealedClassTypeSpec()
  else
    toEnumTypeSpec()
  return listOf(
      QualifiedTypeSpec(packageName = packageName, typeSpec = enumTypeSpec),
      QualifiedTypeSpec(packageName = adapterPackageName(packageName), typeSpec = adapterTypeSpec(asSealedClass))
  )
}

private fun IrEnum.toEnumTypeSpec(): TypeSpec {
  return TypeSpec
      .enumBuilder(name.escapeKotlinReservedWord())
      .applyIf(description?.isNotBlank() == true) { addKdoc("%L\n", description!!) }
      .primaryConstructor(primaryConstructorWithOverriddenParamSpec)
      .addProperty(rawValuePropertySpec)
      .apply {
        values.forEach { value -> addEnumConstant(kotlinNameForEnumValue(value.name), value.typeSpec()) }
        addEnumConstant("UNKNOWN__", unknownEnumConstTypeSpec)
      }
      .build()
}

private fun IrEnum.adapterTypeSpec(asSealedClass: Boolean): TypeSpec {
  val adaptedTypeName = typeName()
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
                      .map { CodeBlock.of("%S -> %L.%L", it.name, kotlinNameForEnum(name), kotlinNameForEnumValue(it.name)) }
                      .joinToCode(separator = "\n", suffix = "\n")
              )
              .add("else -> %L.UNKNOWN__%L\n", name.escapeKotlinReservedWord(), if (asSealedClass) "(rawValue)" else "")
              .endControlFlow()
              .build()
      )
      .addModifiers(KModifier.OVERRIDE)
      .build()
  val toResponseFunSpec = toResponseFunSpecBuilder(adaptedTypeName)
      .addCode("$writer.$value($value.rawValue)")
      .build()

  return TypeSpec
      .objectBuilder(kotlinNameForResponseAdapter(name))
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

private fun IrEnum.toSealedClassTypeSpec(): TypeSpec {
  return TypeSpec
      .classBuilder(kotlinNameForEnum(name))
      .maybeAddDescription(description)
      .addModifiers(KModifier.SEALED)
      .primaryConstructor(primaryConstructorWithOverriddenParamSpec)
      .addProperty(rawValuePropertySpec)
      .addTypes(values.map { value ->
        value.toObjectTypeSpec(ClassName("", kotlinNameForEnum(name)))
      })
      .addType(unknownValueTypeSpec())
      .build()
}

private fun IrEnum.Value.toObjectTypeSpec(superClass: TypeName): TypeSpec {
  return TypeSpec.objectBuilder(kotlinNameForEnumValue(name))
      .applyIf(description?.isNotBlank() == true) { addKdoc("%L\n", description!!) }
      .applyIf(deprecationReason != null) { addAnnotation(deprecatedAnnotation(deprecationReason!!)) }
      .superclass(superClass)
      .addSuperclassConstructorParameter("rawValue = %S", name)
      .build()
}

private fun IrEnum.unknownValueTypeSpec(): TypeSpec {
  return TypeSpec.classBuilder("UNKNOWN__")
      .addKdoc("%L", "Auto generated constant for unknown enum values\n")
      .primaryConstructor(primaryConstructorSpec)
      .superclass(ClassName("", name.escapeKotlinReservedWord()))
      .addSuperclassConstructorParameter("rawValue = rawValue")
      .build()
}
