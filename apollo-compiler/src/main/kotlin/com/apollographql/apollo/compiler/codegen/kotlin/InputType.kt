package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputFieldWriter
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.FieldType
import com.apollographql.apollo.compiler.ast.InputType
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asPropertySpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asTypeName
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.suppressWarningsAnnotation
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.toDefaultValueCodeBlock
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal fun InputType.typeSpec(generateAsInternal: Boolean = false) =
    TypeSpec
        .classBuilder(name)
        .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
        .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
        .addAnnotation(suppressWarningsAnnotation)
        .addModifiers(KModifier.DATA)
        .addSuperinterface(com.apollographql.apollo.api.InputType::class)
        .primaryConstructor(primaryConstructorSpec)
        .addProperties(fields.map { field -> field.asPropertySpec(initializer = CodeBlock.of(field.name)) })
        .addFunction(marshallerFunSpec)
        .build()

private val InputType.primaryConstructorSpec: FunSpec
  get() {
    return FunSpec
        .constructorBuilder()
        .addParameters(fields.map { field -> field.parameterSpec() })
        .build()
  }

private fun InputType.Field.parameterSpec(): ParameterSpec {
  val rawTypeName = type.asTypeName()
  val typeName = when {
    isOptional -> Input::class.asClassName().parameterizedBy(rawTypeName)
    else -> rawTypeName
  }
  val defaultValue = defaultValue
      ?.toDefaultValueCodeBlock(typeName = rawTypeName, fieldType = type)
      .let { code ->
        if (isOptional) {
          code?.let { CodeBlock.of("%T.optional(%L)", Input::class, it) } ?: CodeBlock.of("%T.absent()", Input::class)
        } else {
          code
        }
      }
  return ParameterSpec
      .builder(name = name, type = typeName)
      .applyIf(defaultValue != null) { defaultValue(defaultValue!!) }
      .build()
}

private val InputType.marshallerFunSpec: FunSpec
  get() {
    return FunSpec
        .builder("marshaller")
        .returns(InputFieldMarshaller::class)
        .addModifiers(KModifier.OVERRIDE)
        .addCode(
            CodeBlock.builder()
                .add("return %T { _writer ->\n", InputFieldMarshaller::class)
                .indent()
                .apply { fields.forEach { field -> add(field.writeCodeBlock) } }
                .unindent()
                .add("}\n")
                .build()
        ).build()
  }

internal val InputType.Field.writeCodeBlock: CodeBlock
  get() {
    return when (type) {
      is FieldType.Scalar -> when (type) {
        is FieldType.Scalar.String -> {
          if (isOptional) {
            CodeBlock.of("if (%L.defined) _writer.writeString(%S, %L.value)\n", name, schemaName, name)
          } else {
            CodeBlock.of("_writer.writeString(%S, %L)\n", schemaName, name)
          }
        }
        is FieldType.Scalar.Int -> {
          if (isOptional) {
            CodeBlock.of("if (%L.defined) _writer.writeInt(%S, %L.value)\n", schemaName, name, name)
          } else {
            CodeBlock.of("_writer.writeInt(%S, %L)\n", schemaName, name)
          }
        }
        is FieldType.Scalar.Boolean -> {
          if (isOptional) {
            CodeBlock.of("if (%L.defined) _writer.writeBoolean(%S, %L.value)\n", name, schemaName, name)
          } else {
            CodeBlock.of("_writer.writeBoolean(%S, %L)\n", schemaName, name)
          }
        }
        is FieldType.Scalar.Float -> {
          if (isOptional) {
            CodeBlock.of("if (%L.defined) _writer.writeDouble(%S, %L.value)\n", name, schemaName, name)
          } else {
            CodeBlock.of("_writer.writeDouble(%S, %L)\n", schemaName, name)
          }
        }
        is FieldType.Scalar.Enum -> {
          if (isOptional) {
            CodeBlock.of("if (%L.defined) _writer.writeString(%S, %L.value?.rawValue)\n", name, schemaName, name)
          } else {
            CodeBlock.of("_writer.writeString(%S, %L.rawValue)\n", schemaName, name)
          }
        }
        is FieldType.Scalar.Custom -> {
          if (isOptional) {
            CodeBlock.of("if (%L.defined) _writer.writeCustom(%S, %T.%L, %L.value)\n", name, schemaName,
                type.customEnumType.asTypeName(),
                type.customEnumConst, name)
          } else {
            CodeBlock.of("_writer.writeCustom(%S, %T.%L, %L)\n", schemaName, type.customEnumType.asTypeName(), type.customEnumConst, name)
          }
        }
      }
      is FieldType.Object -> {
        if (isOptional) {
          CodeBlock.of("if (%L.defined) _writer.writeObject(%S, %L.value?.marshaller())\n", name, schemaName, name)
        } else {
          CodeBlock.of("_writer.writeObject(%S, %L.marshaller())\n", schemaName, name)
        }
      }
      is FieldType.Array -> {
        val codeBlockBuilder: CodeBlock.Builder = CodeBlock.Builder()
        if (isOptional) {
          codeBlockBuilder
              .beginControlFlow("if (%L.defined)", name)
              .add("_writer.writeList(%S, %L.value?.let { _value ->\n", schemaName, name)
              .indent()
              .beginControlFlow("%T { _listItemWriter ->", InputFieldWriter.ListWriter::class)
              .beginControlFlow("_value.forEach { _value ->")
              .add(type.rawType.writeListItem(type.isOptional))
              .endControlFlow()
              .endControlFlow()
              .unindent()
              .add("})\n")
              .endControlFlow()
        } else {
          codeBlockBuilder
              .beginControlFlow("_writer.writeList(%S) { _listItemWriter ->", schemaName)
              .applyIf(isOptional) { beginControlFlow("%L?.forEach { _value ->", name) }
              .applyIf(!isOptional) { beginControlFlow("%L.forEach { _value ->", name) }
              .add(type.rawType.writeListItem(type.isOptional))
              .endControlFlow()
              .endControlFlow()
        }

        codeBlockBuilder.build()
      }
      else -> throw IllegalArgumentException("Unsupported input object field type: $type")
    }
  }

private fun FieldType.writeListItem(isOptional: Boolean): CodeBlock {
  return when (this) {
    is FieldType.Scalar -> when (this) {
      is FieldType.Scalar.String -> CodeBlock.of("_listItemWriter.writeString(_value)\n")
      is FieldType.Scalar.Int -> CodeBlock.of("_listItemWriter.writeInt(_value)\n")
      is FieldType.Scalar.Boolean -> CodeBlock.of("_listItemWriter.writeBoolean(_value)\n")
      is FieldType.Scalar.Float -> CodeBlock.of("_listItemWriter.writeDouble(_value)\n")
      is FieldType.Scalar.Enum -> CodeBlock.of("_listItemWriter.writeString(_value%L.rawValue)\n", if (isOptional) "?" else "")
      is FieldType.Scalar.Custom -> CodeBlock.of("_listItemWriter.writeCustom(%T.%L, _value)\n", customEnumType.asTypeName(),
          customEnumConst)
    }
    is FieldType.Object -> {
      CodeBlock.of("_listItemWriter.writeObject(_value%L.marshaller())\n", if (isOptional) "?" else "")
    }
    is FieldType.Array -> CodeBlock.builder()
        .beginControlFlow("_listItemWriter.writeList { _listItemWriter ->")
        .beginControlFlow("_value%L.forEach { _value ->", if (isOptional) "?" else "")
        .add(rawType.writeListItem(this.isOptional))
        .endControlFlow()
        .endControlFlow()
        .build()
    else -> throw IllegalArgumentException("Unsupported input object field type: $this")
  }
}
