package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.Identifier
import com.apollographql.apollo3.compiler.backend.codegen.deprecatedAnnotation
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForEnum
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForEnumValue
import com.apollographql.apollo3.compiler.backend.codegen.toResponseFunSpecBuilder
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.apollographql.apollo3.compiler.unified.IrEnum
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

internal fun IrEnum.typeSpecs(
    generateAsInternal: Boolean = false,
    enumAsSealedClassPatternFilters: List<Regex>,
    packageName: String,
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

private fun IrEnum.toEnumTypeSpec(generateAsInternal: Boolean): TypeSpec {
  return TypeSpec
      .enumBuilder(name.escapeKotlinReservedWord())
      .applyIf(description?.isNotBlank() == true) { addKdoc("%L\n", description!!) }
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .primaryConstructor(primaryConstructorWithOverriddenParamSpec)
      .addProperty(rawValuePropertySpec)
      .apply {
        values.forEach { value -> addEnumConstant(kotlinNameForEnumValue(value.name), value.typeSpec()) }
        addEnumConstant("UNKNOWN__", unknownEnumConstTypeSpec)
      }
      .build()
}

private fun IrEnum.adapterTypeSpec(generateAsInternal: Boolean, asSealedClass: Boolean, packageName: String): TypeSpec {
  val fromResponseFunSpec = FunSpec.builder("fromResponse")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec.builder("reader", JsonReader::class).build())
      .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class)
      .returns(ClassName(packageName, name.escapeKotlinReservedWord()))
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
  val toResponseFunSpec = toResponseFunSpecBuilder(ClassName(packageName, name.escapeKotlinReservedWord()))
      .addCode("writer.value(value.rawValue)")
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

private fun IrEnum.toSealedClassTypeSpec(generateAsInternal: Boolean): TypeSpec {
  return TypeSpec
      .classBuilder(kotlinNameForEnum(name))
      .applyIf(description?.isNotBlank() == true) { addKdoc("%L\n", description!!) }
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
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
