package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseWriter
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.ObjectType.writeToResponseFunSpec(): FunSpec {
  return when (this.kind) {
    is CodeGenerationAst.ObjectType.Kind.Fragment -> writeFragmentToResponseFunSpec()
    is CodeGenerationAst.ObjectType.Kind.FragmentDelegate -> writeFragmentDelegateToResponseFunSpec()
    else -> writeObjectToResponseFunSpec()
  }
}

private fun CodeGenerationAst.ObjectType.writeObjectToResponseFunSpec(): FunSpec {
  val writeFieldsCode = this.fields.mapIndexed { index, field ->
    field.writeCode(responseField = "RESPONSE_FIELDS[$index]")
  }.joinToCode(separator = "")
  return FunSpec.builder("toResponse")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec(name = "writer", type = ResponseWriter::class.asTypeName()))
      .addParameter(ParameterSpec(name = "value", type = this.typeRef.asTypeName()))
      .addCode(writeFieldsCode)
      .build()
}

private fun CodeGenerationAst.ObjectType.writeFragmentDelegateToResponseFunSpec(): FunSpec {
  val fragmentRef = (this.kind as CodeGenerationAst.ObjectType.Kind.FragmentDelegate).fragmentTypeRef
  return FunSpec.builder("toResponse")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec(name = "writer", type = ResponseWriter::class.asTypeName()))
      .addParameter(ParameterSpec(name = "value", type = this.typeRef.asTypeName()))
      .addStatement("%T.toResponse(writer,·value.delegate)", fragmentRef.enclosingType!!.asAdapterTypeName())
      .build()
}

private fun CodeGenerationAst.ObjectType.writeFragmentToResponseFunSpec(): FunSpec {
  val (defaultImplementation, possibleImplementations) = with(this.kind as CodeGenerationAst.ObjectType.Kind.Fragment) {
    defaultImplementation to possibleImplementations
  }
  return FunSpec.builder("toResponse")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec(name = "writer", type = ResponseWriter::class.asTypeName()))
      .addParameter(ParameterSpec(name = "value", type = this.typeRef.asTypeName()))
      .applyIf(possibleImplementations.isEmpty()) {
        addCode(
            this@writeFragmentToResponseFunSpec.fields.mapIndexed { index, field ->
              field.writeCode(responseField = "RESPONSE_FIELDS[$index]")
            }.joinToCode(separator = "")
        )
      }
      .applyIf(possibleImplementations.isNotEmpty()) {
        beginControlFlow("when(value)")
        addCode(
            possibleImplementations
                .values
                .distinct()
                .map { type ->
                  CodeBlock.of(
                      "is·%T·->·%T.toResponse(writer,·value)",
                      type.asTypeName(),
                      type.asAdapterTypeName(),
                  )
                }
                .joinToCode(separator = "\n", suffix = "\n")
        )
        addStatement(
            "is·%T·->·%T.toResponse(writer,·value)",
            defaultImplementation.asTypeName(),
            defaultImplementation.asAdapterTypeName(),
        )
        endControlFlow()
      }
      .build()
}

private fun CodeGenerationAst.Field.writeCode(responseField: String): CodeBlock {
  return when (type) {
    is CodeGenerationAst.FieldType.Scalar -> when (type) {

      is CodeGenerationAst.FieldType.Scalar.ID,
      is CodeGenerationAst.FieldType.Scalar.String -> {
        CodeBlock.of(
            "writer.writeString(%L,·value.%L)\n",
            responseField.escapeKotlinReservedWord(),
            this.name.escapeKotlinReservedWord()
        )
      }

      is CodeGenerationAst.FieldType.Scalar.Int -> {
        CodeBlock.of(
            "writer.writeInt(%L,·value.%L)\n",
            responseField.escapeKotlinReservedWord(),
            this.name.escapeKotlinReservedWord()
        )
      }

      is CodeGenerationAst.FieldType.Scalar.Boolean -> {
        CodeBlock.of(
            "writer.writeBoolean(%L,·value.%L)\n",
            responseField.escapeKotlinReservedWord(),
            this.name.escapeKotlinReservedWord()
        )
      }

      is CodeGenerationAst.FieldType.Scalar.Float -> {
        CodeBlock.of(
            "writer.writeDouble(%L,·value.%L)\n",
            responseField.escapeKotlinReservedWord(),
            this.name.escapeKotlinReservedWord()
        )
      }

      is CodeGenerationAst.FieldType.Scalar.Enum -> {
        if (type.nullable) {
          CodeBlock.of(
              "writer.writeString(%L,·value.%L?.rawValue)\n",
              responseField.escapeKotlinReservedWord(),
              this.name.escapeKotlinReservedWord()
          )
        } else {
          CodeBlock.of(
              "writer.writeString(%L,·value.%L.rawValue)\n",
              responseField.escapeKotlinReservedWord(),
              this.name.escapeKotlinReservedWord()
          )
        }
      }

      is CodeGenerationAst.FieldType.Scalar.Custom -> {
        CodeBlock.of(
            "writer.writeCustom(%L·as·%T,·value.%L)\n",
            responseField.escapeKotlinReservedWord(),
            ResponseField.CustomScalarField::class,
            this.name.escapeKotlinReservedWord()
        )
      }
    }

    is CodeGenerationAst.FieldType.Object -> {
      if (type.nullable) {
        CodeBlock.builder()
            .beginControlFlow("if(value.%L == null)", this.name.escapeKotlinReservedWord())
            .addStatement("writer.writeObject(%L,·null)", responseField.escapeKotlinReservedWord())
            .nextControlFlow("else")
            .beginControlFlow("writer.writeObject(%L)·{·writer·->", responseField.escapeKotlinReservedWord())
            .addStatement(
                "%T.toResponse(writer,·value.%L)",
                this.type.typeRef.asAdapterTypeName(),
                this.name.escapeKotlinReservedWord(),
            )
            .endControlFlow()
            .endControlFlow()
            .build()
      } else {
        CodeBlock.builder()
            .beginControlFlow("writer.writeObject(%L)·{·writer·->", responseField.escapeKotlinReservedWord())
            .addStatement(
                "%T.toResponse(writer,·value.%L)",
                this.type.typeRef.asAdapterTypeName(),
                this.name.escapeKotlinReservedWord(),
            )
            .endControlFlow()
            .build()
      }
    }

    is CodeGenerationAst.FieldType.Array -> {
      CodeBlock.builder()
          .beginControlFlow(
              "writer.writeList(%L,·value.%L)·{·values,·listItemWriter·->",
              responseField.escapeKotlinReservedWord(),
              this.name.escapeKotlinReservedWord(),
          )
          .beginControlFlow("values?.forEach·{·value·->")
          .add(type.writeListItemCode)
          .endControlFlow()
          .endControlFlow()
          .build()
    }
  }
}

private val CodeGenerationAst.FieldType.Array.writeListItemCode: CodeBlock
  get() {
    val safeValue = if (rawType.nullable) "value?" else "value"
    return when (rawType) {
      is CodeGenerationAst.FieldType.Scalar -> when (rawType) {
        is CodeGenerationAst.FieldType.Scalar.ID,
        is CodeGenerationAst.FieldType.Scalar.String -> CodeBlock.of("listItemWriter.writeString(value)")
        is CodeGenerationAst.FieldType.Scalar.Int -> CodeBlock.of("listItemWriter.writeInt(value)")
        is CodeGenerationAst.FieldType.Scalar.Boolean -> CodeBlock.of("listItemWriter.writeBoolean(value)")
        is CodeGenerationAst.FieldType.Scalar.Float -> CodeBlock.of("listItemWriter.writeDouble(value)")
        is CodeGenerationAst.FieldType.Scalar.Enum -> CodeBlock.of("listItemWriter.writeString($safeValue.rawValue)")
        is CodeGenerationAst.FieldType.Scalar.Custom -> CodeBlock.of(
            "listItemWriter.writeCustom(%T,·value)", rawType.customEnumType.asTypeName()
        )
      }
      is CodeGenerationAst.FieldType.Object -> {
        if (rawType.nullable) {
          CodeBlock.builder()
              .beginControlFlow("if(value == null)")
              .addStatement("listItemWriter.writeObject(null)")
              .nextControlFlow("else")
              .beginControlFlow("listItemWriter.writeObject·{·writer·->")
              .addStatement("%T.toResponse(writer,·value)", this.rawType.typeRef.asAdapterTypeName())
              .endControlFlow()
              .endControlFlow()
              .build()
        } else {
          CodeBlock.builder()
              .beginControlFlow("listItemWriter.writeObject·{·writer·->")
              .addStatement("%T.toResponse(writer,·value)", this.rawType.typeRef.asAdapterTypeName())
              .endControlFlow()
              .build()
        }
      }
      is CodeGenerationAst.FieldType.Array -> {
        CodeBlock.builder()
            .beginControlFlow("listItemWriter.writeList(value)·{·value,·listItemWriter·->")
            .beginControlFlow("value?.forEach·{·value·->") // value always nullable in ListItemWriter
            .add(rawType.writeListItemCode)
            .endControlFlow()
            .endControlFlow()
            .build()
      }
    }
  }
