package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.api.CustomScalar
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.ObjectType.readFromResponseFunSpec(): FunSpec {
  return when (this.kind) {
    is CodeGenerationAst.ObjectType.Kind.Fragment -> readFragmentFromResponseFunSpec()
    is CodeGenerationAst.ObjectType.Kind.FragmentDelegate -> readFragmentDelegateFromResponseFunSpec()
    else -> readObjectFromResponseFunSpec()
  }
}

private fun CodeGenerationAst.ObjectType.readObjectFromResponseFunSpec(): FunSpec {
  val fieldVariablesCode = this.fields
      .map { field ->
        if (field.responseName == CodeGenerationAst.typenameField.responseName) {
          CodeBlock.of(
              "var·%L:·%T·=·%L",
              field.name.escapeKotlinReservedWord(),
              field.type.asTypeName().copy(nullable = true),
              CodeGenerationAst.typenameField.responseName.escapeKotlinReservedWord()
          )
        } else {
          CodeBlock.of(
              "var·%L:·%T·=·null",
              field.name.escapeKotlinReservedWord(),
              field.type.asTypeName().copy(nullable = true)
          )
        }
      }
      .joinToCode(separator = "\n", suffix = "\n")

  val selectFieldsCode = CodeBlock.builder()
      .beginControlFlow("while(true)")
      .beginControlFlow("when·(selectField(RESPONSE_FIELDS))")
      .add(
          this.fields.mapIndexed { fieldIndex, field ->
            CodeBlock.of(
                "%L·->·%L·=·%L",
                fieldIndex,
                field.name.escapeKotlinReservedWord(),
                field.type.nullable().fromResponseCode(field = "RESPONSE_FIELDS[$fieldIndex]")
            )
          }.joinToCode(separator = "\n", suffix = "\n")
      )
      .addStatement("else -> break")
      .endControlFlow()
      .endControlFlow()
      .build()

  val typeConstructorCode = CodeBlock.builder()
      .addStatement("%T(", this.typeRef.asTypeName())
      .indent()
      .add(this.fields.map { field ->
        CodeBlock.of(
            "%L·=·%L%L",
            field.name.escapeKotlinReservedWord(),
            field.name.escapeKotlinReservedWord(),
            "!!".takeUnless { field.type.nullable } ?: ""
        )
      }.joinToCode(separator = ",\n", suffix = "\n"))
      .unindent()
      .addStatement(")")
      .build()

  return FunSpec.builder("fromResponse")
      .addModifiers(KModifier.OVERRIDE)
      .returns(this.typeRef.asTypeName())
      .addParameter(ParameterSpec.builder("reader", ResponseReader::class).build())
      .addParameter(CodeGenerationAst.typenameField.asOptionalParameterSpec(withDefaultValue = false))
      .addCode(CodeBlock
          .builder()
          .beginControlFlow("return·reader.run")
          .add(fieldVariablesCode)
          .add(selectFieldsCode)
          .add(typeConstructorCode)
          .endControlFlow()
          .build()
      )
      .build()
}

private fun CodeGenerationAst.ObjectType.readFragmentFromResponseFunSpec(): FunSpec {
  val (defaultImplementation, possibleImplementations) = with(this.kind as CodeGenerationAst.ObjectType.Kind.Fragment) {
    defaultImplementation to possibleImplementations
  }
  return FunSpec.builder("fromResponse")
      .addModifiers(KModifier.OVERRIDE)
      .returns(this.typeRef.asTypeName())
      .addParameter(ParameterSpec.builder("reader", ResponseReader::class).build())
      .addParameter(CodeGenerationAst.typenameField.asOptionalParameterSpec(withDefaultValue = false))
      .applyIf(possibleImplementations.isEmpty()) {
        addStatement(
            "return·%T.fromResponse(reader)",
            defaultImplementation.asAdapterTypeName()
        )
      }
      .applyIf(possibleImplementations.isNotEmpty()) {
        addStatement(
            "val·typename·=·%L·?:·reader.readString(RESPONSE_FIELDS[0])",
            CodeGenerationAst.typenameField.responseName.escapeKotlinReservedWord()
        )
        beginControlFlow("return·when(typename)")
        addCode(
            possibleImplementations
                .map { (typeCondition, type) ->
                  CodeBlock.of(
                      "%S·->·%T.fromResponse(reader,·typename)",
                      typeCondition,
                      type.asAdapterTypeName(),
                  )
                }
                .joinToCode(separator = "\n", suffix = "\n")
        )
        addStatement(
            "else·->·%T.fromResponse(reader,·typename)",
            defaultImplementation.asAdapterTypeName()
        )
        endControlFlow()
      }
      .build()
}

private fun CodeGenerationAst.ObjectType.readFragmentDelegateFromResponseFunSpec(): FunSpec {
  val fragmentRef = (this.kind as CodeGenerationAst.ObjectType.Kind.FragmentDelegate).fragmentTypeRef
  return FunSpec.builder("fromResponse")
      .addModifiers(KModifier.OVERRIDE)
      .returns(this.typeRef.asTypeName())
      .addParameter(ParameterSpec.builder("reader", ResponseReader::class).build())
      .addParameter(CodeGenerationAst.typenameField.asOptionalParameterSpec(withDefaultValue = false))
      .addStatement(
          "return·%T(%T.fromResponse(reader,·%L))",
          this.typeRef.asTypeName(),
          fragmentRef.enclosingType!!.asAdapterTypeName(),
          CodeGenerationAst.typenameField.responseName.escapeKotlinReservedWord()
      )
      .build()
}

private fun CodeGenerationAst.FieldType.fromResponseCode(field: String): CodeBlock {
  val notNullOperator = "!!".takeUnless { nullable } ?: ""
  return when (this) {
    is CodeGenerationAst.FieldType.Scalar -> when (this) {
      is CodeGenerationAst.FieldType.Scalar.ID,
      is CodeGenerationAst.FieldType.Scalar.String -> CodeBlock.of("readString(%L)%L", field, notNullOperator)
      is CodeGenerationAst.FieldType.Scalar.Int -> CodeBlock.of("readInt(%L)%L", field, notNullOperator)
      is CodeGenerationAst.FieldType.Scalar.Boolean -> CodeBlock.of("readBoolean(%L)%L", field, notNullOperator)
      is CodeGenerationAst.FieldType.Scalar.Float -> CodeBlock.of("readDouble(%L)%L", field, notNullOperator)
      is CodeGenerationAst.FieldType.Scalar.Enum -> if (nullable) {
        CodeBlock.of("readString(%L)?.let·{·%T.safeValueOf(it)·}", field, typeRef.asTypeName().copy(nullable = false))
      } else {
        CodeBlock.of("%T.safeValueOf(readString(%L)!!)", typeRef.asTypeName().copy(nullable = false), field)
      }
      is CodeGenerationAst.FieldType.Scalar.Custom -> if (field.isNotEmpty()) {
        CodeBlock.of("readCustomScalar<%T>(%L·as·%T)%L", ClassName.bestGuess(type), field, ResponseField.CustomScalarField::class,
            notNullOperator)
      } else {
        CodeBlock.of(
            "readCustomScalar<%T>(%T)%L", ClassName.bestGuess(type), typeName, notNullOperator
        )
      }
    }
    is CodeGenerationAst.FieldType.Object -> {
      val fieldCode = field.takeIf { it.isNotEmpty() }?.let { CodeBlock.of("(%L)", it) } ?: CodeBlock.of("")
      CodeBlock.builder()
          .addStatement("readObject<%T>%L·{·reader·->", typeRef.asTypeName(), fieldCode)
          .indent()
          .addStatement("%T.fromResponse(reader)", typeRef.asAdapterTypeName())
          .unindent()
          .add("}%L", notNullOperator)
          .build()
    }
    is CodeGenerationAst.FieldType.Array -> {
      CodeBlock.builder()
          .addStatement("readList<%T>(%L)·{·reader·->", rawType.asTypeName().copy(nullable = false), field)
          .indent()
          .add(rawType.readListItemCode())
          .unindent()
          .add("\n}%L", notNullOperator)
          .applyIf(!rawType.nullable) {
            if (nullable) {
              add("?.map·{ it!! }")
            } else {
              add(".map·{ it!! }")
            }
          }
          .build()
    }
  }
}

private fun CodeGenerationAst.FieldType.readListItemCode(): CodeBlock {
  return when (this) {
    is CodeGenerationAst.FieldType.Scalar -> when (this) {
      is CodeGenerationAst.FieldType.Scalar.ID,
      is CodeGenerationAst.FieldType.Scalar.String -> CodeBlock.of("reader.readString()")
      is CodeGenerationAst.FieldType.Scalar.Int -> CodeBlock.of("reader.readInt()")
      is CodeGenerationAst.FieldType.Scalar.Boolean -> CodeBlock.of("reader.readBoolean()")
      is CodeGenerationAst.FieldType.Scalar.Float -> CodeBlock.of("reader.readDouble()")
      is CodeGenerationAst.FieldType.Scalar.Enum -> CodeBlock.of(
          "%T.safeValueOf(reader.readString())", typeRef.asTypeName().copy(nullable = false)
      )
      is CodeGenerationAst.FieldType.Scalar.Custom -> CodeBlock.of(
          "reader.readCustomScalar<%T>(%T)", ClassName.bestGuess(type), typeName
      )
    }
    is CodeGenerationAst.FieldType.Object -> {
      CodeBlock.builder()
          .addStatement("reader.readObject<%T>·{·reader·->", typeRef.asTypeName())
          .indent()
          .addStatement("%T.fromResponse(reader)", typeRef.asAdapterTypeName())
          .unindent()
          .add("}")
          .build()
    }
    is CodeGenerationAst.FieldType.Array -> {
      CodeBlock.builder()
          .addStatement("reader.readList<%T>·{·reader·->", rawType.asTypeName().copy(nullable = false))
          .indent()
          .add(rawType.readListItemCode())
          .unindent()
          .add("\n}")
          .applyIf(!rawType.nullable) { add(".map·{ it!! }") }
          .build()
    }
  }
}
