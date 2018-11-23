package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputFieldWriter
import com.apollographql.apollo.api.InputType
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.AST
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asTypeName
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

fun KotlinCodeGen.inputTypeSpec(inputType: AST.InputType): TypeSpec {
  return with(inputType) {
    TypeSpec.classBuilder(name)
      .addAnnotation(generatedByApolloAnnotation)
      .addAnnotation(suppressWarningsAnnotation)
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .addSuperinterface(InputType::class)
      .primaryConstructor(primaryConstructorSpec)
      .addProperties(fields.map { field -> field.asPropertySpec(initializer = CodeBlock.of(field.name)) })
      .addFunction(marshallerFunSpec)
      .build()
  }
}

private val AST.InputType.primaryConstructorSpec: FunSpec
  get() {
    return FunSpec.constructorBuilder()
      .addParameters(fields.map { field -> field.parameterSpec() })
      .build()
  }

private fun AST.InputType.Field.parameterSpec(): ParameterSpec {
  val typeName = type.asTypeName()
  return ParameterSpec.builder(
    name = name,
    type = if (isOptional) Input::class.asClassName().parameterizedBy(typeName) else typeName
  ).apply {
    if (isOptional) {
      defaultValue(
        CodeBlock.of("%T.optional(%L)", Input::class, defaultValue?.toDefaultValueCodeBlock(
          typeName = typeName,
          fieldType = type
        ))
      )
    }
  }.build()
}

private val AST.InputType.marshallerFunSpec: FunSpec
  get() {
    return FunSpec.builder("marshaller")
      .returns(InputFieldMarshaller::class)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(
        CodeBlock.builder()
          .add("return %T { writer ->\n", InputFieldMarshaller::class)
          .indent()
          .apply { fields.forEach { field -> add(field.writeCodeBlock) } }
          .unindent()
          .add("}\n")
          .build()
      ).build()
  }

val AST.InputType.Field.writeCodeBlock: CodeBlock
  get() {
    return when (type) {
      is AST.FieldType.Scalar -> when (type) {
        is AST.FieldType.Scalar.String -> {
          if (isOptional) {
            CodeBlock.of("if (%L.defined) writer.writeString(%S, %L.value)\n", name, schemaName, name)
          } else {
            CodeBlock.of("writer.writeString(%S, %L)\n", schemaName, name)
          }
        }
        is AST.FieldType.Scalar.Int -> {
          if (isOptional) {
            CodeBlock.of("if (%L.defined) writer.writeInt(%S, %L.value)\n", schemaName, name, name)
          } else {
            CodeBlock.of("writer.writeInt(%S, %L)\n", schemaName, name)
          }
        }
        is AST.FieldType.Scalar.Boolean -> {
          if (isOptional) {
            CodeBlock.of("if (%L.defined) writer.writeBoolean(%S, %L.value)\n", name, schemaName, name)
          } else {
            CodeBlock.of("writer.writeBoolean(%S, %L)\n", schemaName, name)
          }
        }
        is AST.FieldType.Scalar.Float -> {
          if (isOptional) {
            CodeBlock.of("if (%L.defined) writer.writeDouble(%S, %L.value)\n", name, schemaName, name)
          } else {
            CodeBlock.of("writer.writeDouble(%S, %L)\n", schemaName, name)
          }
        }
        is AST.FieldType.Scalar.Enum -> {
          if (isOptional) {
            CodeBlock.of("if (%L.defined) writer.writeString(%S, %L.value?.rawValue)\n", name, schemaName, name)
          } else {
            CodeBlock.of("writer.writeString(%S, %L.rawValue)\n", schemaName, name)
          }
        }
        is AST.FieldType.Scalar.Custom -> {
          if (isOptional) {
            CodeBlock.of("if (%L.defined) writer.writeCustom(%S, %T.%L, %L.value)\n", name, schemaName, type.customEnumType.asTypeName(),
              type.customEnumConst, name)
          } else {
            CodeBlock.of("writer.writeCustom(%S, %T.%L, %L)\n", schemaName, type.customEnumType.asTypeName(), type.customEnumConst, name)
          }
        }
      }
      is AST.FieldType.Object -> {
        if (isOptional) {
          CodeBlock.of("if (%L.defined) writer.writeString(%S, %L.value?.marshaller())\n", name, schemaName, name)
        } else {
          CodeBlock.of("writer.writeObject(%S, %L.marshaller())\n", schemaName, name)
        }
      }
      is AST.FieldType.Array -> {
        val codeBlockBuilder: CodeBlock.Builder = CodeBlock.Builder()
        if (isOptional) {
          codeBlockBuilder
            .beginControlFlow("if (%L.defined)", name)
            .add("writer.writeList(%S, %L.value?.let { value ->\n", schemaName, name)
            .indent()
            .add("%T { listItemWriter ->\n", InputFieldWriter.ListWriter::class)
            .indent()
            .add("value.forEach { value ->\n")
            .indent()
            .add(type.rawType.writeListItem)
            .unindent()
            .add("}\n")
            .unindent()
            .add("}\n")
            .unindent()
            .add("})\n")
            .endControlFlow()
        } else {
          codeBlockBuilder
            .add("writer.writeList(%S) { listItemWriter ->\n", schemaName)
            .indent()
            .add("%L?.forEach { value ->\n", name)
            .indent()
            .add(type.rawType.writeListItem)
            .unindent()
            .add("}\n")
            .unindent()
            .add("}\n")
        }

        codeBlockBuilder.build()
      }
      else -> throw IllegalArgumentException("Unsupported input object field type: $type")
    }
  }

private val AST.FieldType.writeListItem: CodeBlock
  get() {
    return when (this) {
      is AST.FieldType.Scalar -> when (this) {
        is AST.FieldType.Scalar.String -> CodeBlock.of("listItemWriter.writeString(value)\n")
        is AST.FieldType.Scalar.Int -> CodeBlock.of("listItemWriter.writeInt(value)\n")
        is AST.FieldType.Scalar.Boolean -> CodeBlock.of("listItemWriter.writeBoolean(value)\n")
        is AST.FieldType.Scalar.Float -> CodeBlock.of("listItemWriter.writeDouble(value)\n")
        is AST.FieldType.Scalar.Enum -> CodeBlock.of("listItemWriter.writeString(value?.rawValue)\n")
        is AST.FieldType.Scalar.Custom -> CodeBlock.of("listItemWriter.writeCustom(%T.%L, value)\n",
          customEnumType.asTypeName(), customEnumConst)
      }
      is AST.FieldType.Object -> CodeBlock.of("listItemWriter.writeObject(value?.marshaller())\n")
      is AST.FieldType.Array -> CodeBlock.builder()
        .add("listItemWriter.writeList{ listItemWriter-> \n")
        .indent()
        .add("value?.forEach { value ->\n")
        .indent()
        .add(rawType.writeListItem)
        .unindent()
        .add("}\n")
        .unindent()
        .add("}\n")
        .build()
      else -> throw IllegalArgumentException("Unsupported input object field type: $this")
    }
  }