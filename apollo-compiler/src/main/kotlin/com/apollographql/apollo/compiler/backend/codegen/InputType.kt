package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.api.CustomScalar
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.InputFieldWriter
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

internal fun CodeGenerationAst.InputField.asPropertySpec(initializer: CodeBlock): PropertySpec {
  return PropertySpec
      .builder(
          name = name.escapeKotlinReservedWord(),
          type = type.asTypeName().let { type ->
            type.takeUnless { type.isNullable } ?: Input::class.asClassName().parameterizedBy(type.copy(nullable = false))
          }
      )
      .apply { if (description.isNotBlank()) addKdoc("%L\n", description) }
      .apply { initializer(initializer) }
      .build()
}

internal fun CodeGenerationAst.InputType.typeSpec(generateAsInternal: Boolean = false) =
    TypeSpec
        .classBuilder(name.escapeKotlinReservedWord())
        .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
        .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
        .addAnnotation(suppressWarningsAnnotation)
        .addModifiers(KModifier.DATA)
        .addSuperinterface(com.apollographql.apollo.api.InputType::class)
        .primaryConstructor(primaryConstructorSpec)
        .addProperties(
            fields.map { field ->
              field.asPropertySpec(
                  initializer = CodeBlock.of(field.name.escapeKotlinReservedWord())
              )
            }
        )
        .addFunction(marshallerFunSpec)
        .build()

private val CodeGenerationAst.InputType.primaryConstructorSpec: FunSpec
  get() {
    return FunSpec
        .constructorBuilder()
        .addParameters(fields.map { field -> field.parameterSpec() })
        .build()
  }

private fun CodeGenerationAst.InputField.parameterSpec(): ParameterSpec {
  val rawTypeName = type.asTypeName()
  val typeName = type.asTypeName().let { type ->
    type.takeUnless { type.isNullable } ?: Input::class.asClassName().parameterizedBy(type.copy(nullable = false))
  }
  val defaultValue = defaultValue
      ?.toDefaultValueCodeBlock(typeName = rawTypeName, fieldType = type)
      .let { code ->
        if (type.nullable) {
          code?.let { CodeBlock.of("%T.optional(%L)", Input::class, it) } ?: CodeBlock.of("%T.absent()", Input::class)
        } else {
          code
        }
      }
  return ParameterSpec
      .builder(name = name.escapeKotlinReservedWord(), type = typeName)
      .applyIf(defaultValue != null) { defaultValue(defaultValue!!) }
      .build()
}

private val CodeGenerationAst.InputType.marshallerFunSpec: FunSpec
  get() {
    return FunSpec
        .builder("marshaller")
        .returns(InputFieldMarshaller::class)
        .addModifiers(KModifier.OVERRIDE)
        .addCode(CodeBlock
            .builder()
            .beginControlFlow("return %T.invoke { writer ->", InputFieldMarshaller::class)
            .apply {
              fields.forEach { field ->
                add(field.writeCodeBlock(name.escapeKotlinReservedWord()))
              }
            }
            .endControlFlow()
            .build()
        ).build()
  }

internal fun CodeGenerationAst.InputField.writeCodeBlock(thisRef: String): CodeBlock {
  return when (type) {
    is CodeGenerationAst.FieldType.Scalar -> when (type) {
      is CodeGenerationAst.FieldType.Scalar.ID,
      is CodeGenerationAst.FieldType.Scalar.String -> {
        if (type.nullable) {
          CodeBlock.builder()
              .addStatement("if·(this@%L.%L.defined)·{", thisRef, name.escapeKotlinReservedWord())
              .indent()
              .addStatement("writer.writeString(%S, this@%L.%L.value)", schemaName, thisRef, name.escapeKotlinReservedWord())
              .unindent()
              .addStatement("}")
              .build()
        } else {
          CodeBlock.of("writer.writeString(%S, this@%L.%L)\n", schemaName, thisRef, name.escapeKotlinReservedWord())
        }
      }
      is CodeGenerationAst.FieldType.Scalar.Int -> {
        if (type.nullable) {
          CodeBlock.builder()
              .addStatement("if·(this@%L.%L.defined)·{", thisRef, name.escapeKotlinReservedWord())
              .indent()
              .addStatement(
                  "writer.writeInt(%S, this@%L.%L.value)",
                  schemaName,
                  thisRef,
                  name.escapeKotlinReservedWord()
              )
              .unindent()
              .addStatement("}")
              .build()
        } else {
          CodeBlock.of("writer.writeInt(%S, this@%L.%L)\n", schemaName, thisRef, name.escapeKotlinReservedWord())
        }
      }
      is CodeGenerationAst.FieldType.Scalar.Boolean -> {
        if (type.nullable) {
          CodeBlock.builder()
              .addStatement("if·(this@%L.%L.defined)·{", thisRef, name.escapeKotlinReservedWord())
              .indent()
              .addStatement("writer.writeBoolean(%S, this@%L.%L.value)", schemaName, thisRef, name.escapeKotlinReservedWord())
              .unindent()
              .addStatement("}")
              .build()
        } else {
          CodeBlock.of("writer.writeBoolean(%S, this@%L.%L)\n", schemaName, thisRef, name.escapeKotlinReservedWord())
        }
      }
      is CodeGenerationAst.FieldType.Scalar.Float -> {
        if (type.nullable) {
          CodeBlock.builder()
              .addStatement("if·(this@%L.%L.defined)·{", thisRef, name.escapeKotlinReservedWord())
              .indent()
              .addStatement("writer.writeDouble(%S, this@%L.%L.value)", schemaName, thisRef, name.escapeKotlinReservedWord())
              .unindent()
              .addStatement("}")
              .build()
        } else {
          CodeBlock.of("writer.writeDouble(%S, this@%L.%L)\n", schemaName, thisRef, name.escapeKotlinReservedWord())
        }
      }
      is CodeGenerationAst.FieldType.Scalar.Enum -> {
        if (type.nullable) {
          CodeBlock.builder()
              .addStatement("if·(this@%L.%L.defined)·{", thisRef, name.escapeKotlinReservedWord())
              .indent()
              .addStatement("writer.writeString(%S, this@%L.%L.value?.rawValue)", schemaName, thisRef, name.escapeKotlinReservedWord())
              .unindent()
              .addStatement("}")
              .build()
        } else {
          CodeBlock.of("writer.writeString(%S, this@%L.%L.rawValue)\n", schemaName, thisRef, name.escapeKotlinReservedWord())
        }
      }
      is CodeGenerationAst.FieldType.Scalar.Custom -> {
        if (type.nullable) {
          CodeBlock.builder()
              .addStatement("if·(this@%L.%L.defined)·{", thisRef, name)
              .indent()
              .addStatement(
                  "writer.writeCustom(%S, %T, this@%L.%L.value)",
                  schemaName,
                  type.typeName,
                  thisRef,
                  name.escapeKotlinReservedWord()
              )
              .unindent()
              .addStatement("}")
              .build()
        } else {
          CodeBlock.of(
              "writer.writeCustom(%S, %T, this@%L.%L)\n",
              schemaName,
              type.typeName,
              thisRef,
              name.escapeKotlinReservedWord()
          )
        }
      }
    }
    is CodeGenerationAst.FieldType.Object -> {
      if (type.nullable) {
        CodeBlock.builder()
            .addStatement("if·(this@%L.%L.defined)·{", thisRef, name.escapeKotlinReservedWord())
            .indent()
            .addStatement("writer.writeObject(%S, this@%L.%L.value?.marshaller())", schemaName, thisRef, name.escapeKotlinReservedWord())
            .unindent()
            .addStatement("}")
            .build()
      } else {
        CodeBlock.of("writer.writeObject(%S, this@%L.%L.marshaller())\n", schemaName, thisRef, name.escapeKotlinReservedWord())
      }
    }
    is CodeGenerationAst.FieldType.Array -> {
      val codeBlockBuilder: CodeBlock.Builder = CodeBlock.Builder()
      if (type.nullable) {
        codeBlockBuilder
            .beginControlFlow("if·(this@%L.%L.defined)", thisRef, name)
            .add("writer.writeList(%S, this@%L.%L.value?.let { value ->\n", schemaName, thisRef, name.escapeKotlinReservedWord())
            .indent()
            .beginControlFlow("%T { listItemWriter ->", InputFieldWriter.ListWriter::class)
            .beginControlFlow("value.forEach·{ value ->")
            .add(type.rawType.writeListItem())
            .endControlFlow()
            .endControlFlow()
            .unindent()
            .add("})\n")
            .endControlFlow()
      } else {
        codeBlockBuilder
            .beginControlFlow("writer.writeList(%S) { listItemWriter ->", schemaName)
            .beginControlFlow("this@%L.%L.forEach·{ value ->", thisRef, name.escapeKotlinReservedWord())
            .add(type.rawType.writeListItem())
            .endControlFlow()
            .endControlFlow()
      }
      codeBlockBuilder.build()
    }
  }
}

private fun CodeGenerationAst.FieldType.writeListItem(): CodeBlock {
  return when (this) {
    is CodeGenerationAst.FieldType.Scalar -> when (this) {
      is CodeGenerationAst.FieldType.Scalar.ID,
      is CodeGenerationAst.FieldType.Scalar.String -> CodeBlock.of("listItemWriter.writeString(value)\n")
      is CodeGenerationAst.FieldType.Scalar.Int -> CodeBlock.of("listItemWriter.writeInt(value)\n")
      is CodeGenerationAst.FieldType.Scalar.Boolean -> CodeBlock.of("listItemWriter.writeBoolean(value)\n")
      is CodeGenerationAst.FieldType.Scalar.Float -> CodeBlock.of("listItemWriter.writeDouble(value)\n")
      is CodeGenerationAst.FieldType.Scalar.Enum -> CodeBlock.of(
          "listItemWriter.writeString(value%L.rawValue)\n", if (nullable) "?" else ""
      )
      is CodeGenerationAst.FieldType.Scalar.Custom -> CodeBlock.of(
          "listItemWriter.writeCustom(%T, value)\n", typeName
      )
    }
    is CodeGenerationAst.FieldType.Object -> {
      CodeBlock.of("listItemWriter.writeObject(value%L.marshaller())\n", if (nullable) "?" else "")
    }
    is CodeGenerationAst.FieldType.Array -> CodeBlock.builder()
        .beginControlFlow("listItemWriter.writeList { listItemWriter ->")
        .beginControlFlow("value%L.forEach·{ value ->", if (nullable) "?" else "")
        .add(rawType.writeListItem())
        .endControlFlow()
        .endControlFlow()
        .build()
  }
}
