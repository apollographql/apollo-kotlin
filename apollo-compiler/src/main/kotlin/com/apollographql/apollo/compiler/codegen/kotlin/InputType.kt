package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.InputFieldWriter
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

private fun FieldType.leafType(): FieldType {
  return when (this) {
    is FieldType.Array -> this.rawType
    else -> this
  }
}

private fun Any?.isEmptyList(): Boolean {
  return if (this is List<*>) {
    if (isEmpty()) {
      true
    } else {
      first()?.isEmptyList() == true
    }
  } else {
    false
  }
}

private fun InputType.Field.parameterSpec(): ParameterSpec {
  val rawTypeName = type.asTypeName()
  val typeName = when {
    isOptional -> Input::class.asClassName().parameterizedBy(rawTypeName)
    else -> rawTypeName
  }
  val defaultValue = defaultValue
      /**
       * For input objects, do not try to create a defaultValue
       * See https://github.com/apollographql/apollo-android/issues/3394
       */
      ?.takeIf { type.leafType() !is FieldType.Object || it.isEmptyList() }
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
        .addCode(CodeBlock
            .builder()
            .beginControlFlow("return %T.invoke { writer ->", InputFieldMarshaller::class)
            .apply { fields.forEach { field -> add(field.writeCodeBlock(name)) } }
            .endControlFlow()
            .build()
        ).build()
  }

internal fun InputType.Field.writeCodeBlock(thisRef: String): CodeBlock {
  return when (type) {
    is FieldType.Scalar -> when (type) {
      is FieldType.Scalar.String -> {
        if (isOptional) {
          CodeBlock.builder()
              .addStatement("if·(this@%L.%L.defined)·{", thisRef, name)
              .indent()
              .addStatement("writer.writeString(%S, this@%L.%L.value)", schemaName, thisRef, name)
              .unindent()
              .addStatement("}")
              .build()
        } else {
          CodeBlock.of("writer.writeString(%S, this@%L.%L)\n", schemaName, thisRef, name)
        }
      }
      is FieldType.Scalar.Int -> {
        if (isOptional) {
          CodeBlock.builder()
              .addStatement("if·(this@%L.%L.defined)·{", thisRef, name)
              .indent()
              .addStatement("writer.writeInt(%S, this@%L.%L.value)", schemaName, thisRef, name)
              .unindent()
              .addStatement("}")
              .build()
        } else {
          CodeBlock.of("writer.writeInt(%S, this@%L.%L)\n", schemaName, thisRef, name)
        }
      }
      is FieldType.Scalar.Boolean -> {
        if (isOptional) {
          CodeBlock.builder()
              .addStatement("if·(this@%L.%L.defined)·{", thisRef, name)
              .indent()
              .addStatement("writer.writeBoolean(%S, this@%L.%L.value)", schemaName, thisRef, name)
              .unindent()
              .addStatement("}")
              .build()
        } else {
          CodeBlock.of("writer.writeBoolean(%S, this@%L.%L)\n", schemaName, thisRef, name)
        }
      }
      is FieldType.Scalar.Float -> {
        if (isOptional) {
          CodeBlock.builder()
              .addStatement("if·(this@%L.%L.defined)·{", thisRef, name)
              .indent()
              .addStatement("writer.writeDouble(%S, this@%L.%L.value)", schemaName, thisRef, name)
              .unindent()
              .addStatement("}")
              .build()
        } else {
          CodeBlock.of("writer.writeDouble(%S, this@%L.%L)\n", schemaName, thisRef, name)
        }
      }
      is FieldType.Scalar.Enum -> {
        if (isOptional) {
          CodeBlock.builder()
              .addStatement("if·(this@%L.%L.defined)·{", thisRef, name)
              .indent()
              .addStatement("writer.writeString(%S, this@%L.%L.value?.rawValue)", schemaName, thisRef, name)
              .unindent()
              .addStatement("}")
              .build()
        } else {
          CodeBlock.of("writer.writeString(%S, this@%L.%L.rawValue)\n", schemaName, thisRef, name)
        }
      }
      is FieldType.Scalar.Custom -> {
        if (isOptional) {
          CodeBlock.builder()
              .addStatement("if·(this@%L.%L.defined)·{", thisRef, name)
              .indent()
              .addStatement(
                  "writer.writeCustom(%S, %T.%L, this@%L.%L.value)", schemaName, type.customEnumType.asTypeName(), type.customEnumConst,
                  thisRef, name
              )
              .unindent()
              .addStatement("}")
              .build()
        } else {
          CodeBlock.of("writer.writeCustom(%S, %T.%L, this@%L.%L)\n", schemaName, type.customEnumType.asTypeName(), type.customEnumConst,
              thisRef, name)
        }
      }
    }
    is FieldType.Object -> {
      if (isOptional) {
        CodeBlock.builder()
            .addStatement("if·(this@%L.%L.defined)·{", thisRef, name)
            .indent()
            .addStatement("writer.writeObject(%S, this@%L.%L.value?.marshaller())", schemaName, thisRef, name)
            .unindent()
            .addStatement("}")
            .build()
      } else {
        CodeBlock.of("writer.writeObject(%S, this@%L.%L.marshaller())\n", schemaName, thisRef, name)
      }
    }
    is FieldType.Array -> {
      val codeBlockBuilder: CodeBlock.Builder = CodeBlock.Builder()
      if (isOptional) {
        codeBlockBuilder
            .beginControlFlow("if·(this@%L.%L.defined)", thisRef, name)
            .add("writer.writeList(%S, this@%L.%L.value?.let { value ->\n", schemaName, thisRef, name)
            .indent()
            .beginControlFlow("%T { listItemWriter ->", InputFieldWriter.ListWriter::class)
            .beginControlFlow("value.forEach·{ value ->")
            .add(type.rawType.writeListItem(type.isOptional))
            .endControlFlow()
            .endControlFlow()
            .unindent()
            .add("})\n")
            .endControlFlow()
      } else {
        codeBlockBuilder
            .beginControlFlow("writer.writeList(%S) { listItemWriter ->", schemaName)
            .beginControlFlow("this@%L.%L.forEach·{ value ->", thisRef, name)
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
      is FieldType.Scalar.String -> CodeBlock.of("listItemWriter.writeString(value)\n")
      is FieldType.Scalar.Int -> CodeBlock.of("listItemWriter.writeInt(value)\n")
      is FieldType.Scalar.Boolean -> CodeBlock.of("listItemWriter.writeBoolean(value)\n")
      is FieldType.Scalar.Float -> CodeBlock.of("listItemWriter.writeDouble(value)\n")
      is FieldType.Scalar.Enum -> CodeBlock.of("listItemWriter.writeString(value%L.rawValue)\n", if (isOptional) "?" else "")
      is FieldType.Scalar.Custom -> CodeBlock.of("listItemWriter.writeCustom(%T.%L, value)\n", customEnumType.asTypeName(), customEnumConst)
    }
    is FieldType.Object -> {
      CodeBlock.of("listItemWriter.writeObject(value%L.marshaller())\n", if (isOptional) "?" else "")
    }
    is FieldType.Array -> CodeBlock.builder()
        .beginControlFlow("listItemWriter.writeList { listItemWriter ->")
        .beginControlFlow("value%L.forEach·{ value ->", if (isOptional) "?" else "")
        .add(rawType.writeListItem(this.isOptional))
        .endControlFlow()
        .endControlFlow()
        .build()
    else -> throw IllegalArgumentException("Unsupported input object field type: $this")
  }
}
