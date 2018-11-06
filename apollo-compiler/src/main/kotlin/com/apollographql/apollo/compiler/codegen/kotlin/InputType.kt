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
import com.squareup.kotlinpoet.jvm.throws
import java.io.IOException

fun KotlinCodeGen.inputTypeSpec(inputType: AST.InputType): TypeSpec {
  return with(inputType) {
    TypeSpec.classBuilder(name)
        .addAnnotation(generatedByApolloAnnotation)
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
        .addStatement("return %L", TypeSpec.anonymousClassBuilder()
            .addSuperinterface(InputFieldMarshaller::class)
            .addFunction(marshalFunSpec)
            .build()
        ).build()
  }

private val AST.InputType.marshalFunSpec: FunSpec
  get() {
    return FunSpec.builder("marshal")
        .addModifiers(KModifier.OVERRIDE)
        .throws(IOException::class)
        .addParameter(ParameterSpec.builder("writer", InputFieldWriter::class.java).build())
        .apply { fields.forEach { field -> addCode(field.writeCodeBlock) } }
        .build()
  }

val AST.InputType.Field.writeCodeBlock: CodeBlock
  get() {
    return when (type) {
      is AST.FieldType.Scalar -> when (type) {
        is AST.FieldType.Scalar.String -> CodeBlock.of("writer.writeString(%S, %L)\n", schemaName, name)
        is AST.FieldType.Scalar.Int -> CodeBlock.of("writer.writeInt(%S, %L)\n", schemaName, name)
        is AST.FieldType.Scalar.Boolean -> CodeBlock.of("writer.writeBoolean(%S, %L)\n", schemaName, name)
        is AST.FieldType.Scalar.Float -> CodeBlock.of("writer.writeDouble(%S, %L)\n", schemaName, name)
        is AST.FieldType.Scalar.Enum -> {
          if (isOptional) {
            CodeBlock.of("if (%L.defined) writer.writeString(%S, %L.value?.rawValue)\n", schemaName, name,
                name)
          } else {
            CodeBlock.of("writer.writeString(%S, %L.value.rawValue)\n", schemaName, name)
          }
        }
        is AST.FieldType.Scalar.Custom -> CodeBlock.of("writer.writeCustom(%S, %T.%L, %L.value)\n", schemaName,
            type.customEnumType.asTypeName(), type.customEnumConst, name)
      }
      is AST.FieldType.Object -> {
        if (isOptional) {
          CodeBlock.of("if (%L.defined) writer.writeString(%S, %L.value?.marshaller())\n", schemaName, name,
              name)
        } else {
          CodeBlock.of("writer.writeObject(%S, %L.marshaller())\n", schemaName, name)
        }
      }
      is AST.FieldType.Array -> {
        val codeBlockBuilder: CodeBlock.Builder = CodeBlock.Builder()
        if (isOptional) {
          codeBlockBuilder
              .beginControlFlow("if (%L.defined)", name)
              .add("writer.writeList(%S, %L.value?.let {\n", schemaName, name)
              .indent()
              .add("%L", TypeSpec.anonymousClassBuilder()
                  .addSuperinterface(InputFieldWriter.ListWriter::class)
                  .addFunction(FunSpec.builder("write")
                      .addModifiers(KModifier.OVERRIDE)
                      .addParameter(
                          ParameterSpec.builder("listItemWriter", InputFieldWriter.ListItemWriter::class).build())
                      .beginControlFlow("it.foreach")
                      .addCode(type.rawType.writeListItem)
                      .endControlFlow()
                      .build()
                  )
                  .build()
              )
              .unindent()
              .add("\n} ?: null)\n")
              .endControlFlow()
              .build()
        } else {
          codeBlockBuilder
              .add("writer.writeList(%S, %L", schemaName, TypeSpec.anonymousClassBuilder()
                  .addSuperinterface(InputFieldWriter.ListWriter::class)
                  .addFunction(FunSpec.builder("write")
                      .addModifiers(KModifier.OVERRIDE)
                      .addParameter(
                          ParameterSpec.builder("listItemWriter", InputFieldWriter.ListItemWriter::class).build())
                      .beginControlFlow("%L.foreach", name)
                      .addCode(type.rawType.writeListItem)
                      .endControlFlow()
                      .build()
                  )
                  .build()
              )
              .add(")\n")
              .build()
        }
      }
      else -> throw IllegalArgumentException("Unsupported input object field type: $type")
    }
  }

private val AST.FieldType.writeListItem: CodeBlock
  get() {
    return when (this) {
      is AST.FieldType.Scalar -> when (this) {
        is AST.FieldType.Scalar.String -> CodeBlock.of("listItemWriter.writeString(it)\n")
        is AST.FieldType.Scalar.Int -> CodeBlock.of("listItemWriter.writeInt(it)\n")
        is AST.FieldType.Scalar.Boolean -> CodeBlock.of("listItemWriter.writeBoolean(it)\n")
        is AST.FieldType.Scalar.Float -> CodeBlock.of("listItemWriter.writeDouble(it)\n")
        is AST.FieldType.Scalar.Enum -> CodeBlock.of("listItemWriter.writeString(it?.rawValue)\n")
        is AST.FieldType.Scalar.Custom -> CodeBlock.of("listItemWriter.writeCustom(%T.%L, it)\n",
            customEnumType.asTypeName(), customEnumConst)
      }
      is AST.FieldType.Object -> CodeBlock.of("listItemWriter.writeObject(it.marshaller())\n")
      is AST.FieldType.Array -> CodeBlock.of("listItemWriter.writeList(%L)\n", TypeSpec.anonymousClassBuilder()
          .addSuperinterface(InputFieldWriter.ListWriter::class)
          .addFunction(FunSpec.builder("write")
              .addModifiers(KModifier.OVERRIDE)
              .addParameter(ParameterSpec.builder("listItemWriter", InputFieldWriter.ListItemWriter::class).build())
              .beginControlFlow("it.foreach")
              .addCode(rawType.writeListItem)
              .endControlFlow()
              .build()
          )
          .build()
      )
      else -> throw IllegalArgumentException("Unsupported input object field type: $this")
    }
  }