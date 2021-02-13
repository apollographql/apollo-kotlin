package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.backend.ast.toLowerCamelCase
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.exception.UnexpectedNullValue
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
      .beginControlFlow("when·(reader.selectName(RESPONSE_NAMES))")
      .add(
          this.fields.mapIndexed { fieldIndex, field ->
            CodeBlock.of(
                "%L·->·%L·=·%L",
                fieldIndex,
                field.name.escapeKotlinReservedWord(),
                field.type.fromResponseCode(field.name)
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
      .addParameter(ParameterSpec.builder("reader", JsonReader::class).build())
      .applyIf(isTypeCase) { addParameter(CodeGenerationAst.typenameField.asOptionalParameterSpec(withDefaultValue = false)) }
      .addCode(CodeBlock
          .builder()
          .add(fieldVariablesCode)
          .applyIf(!isTypeCase) {addStatement("reader.beginObject()")}
          .add(selectFieldsCode)
          .applyIf(!isTypeCase) {addStatement("reader.endObject()")}
          .add("return·%L", typeConstructorCode)
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
      .addParameter(ParameterSpec.builder("reader", JsonReader::class).build())
      .addParameter(CodeGenerationAst.typenameField.asOptionalParameterSpec(withDefaultValue = false))
      .applyIf(possibleImplementations.isEmpty()) {
        addStatement(
            "return·%T.fromResponse(reader)",
            defaultImplementation.asAdapterTypeName()
        )
      }
      .applyIf(possibleImplementations.isNotEmpty()) {
        addStatement("reader.beginObject()")
        addStatement("check(reader.nextName() == \"__typename\")")
        addStatement("val·typename·=·reader.nextString()")
        addStatement(
            "",
            CodeGenerationAst.typenameField.responseName.escapeKotlinReservedWord(),
            ResponseField::class.java
        )
        beginControlFlow("return·when(typename)")
        addCode(
            possibleImplementations
                .map { (typeCondition, typeRef) ->
                  CodeBlock.of(
                      "%S·->·${typeRef.name.toLowerCamelCase()}Adapter.fromResponse(reader,·typename)",
                      typeCondition,
                  )
                }
                .joinToCode(separator = "\n", suffix = "\n")
        )
        addStatement(
            "else·->·${defaultImplementation.name.toLowerCamelCase()}Adapter.fromResponse(reader,·typename)",
            defaultImplementation.asAdapterTypeName()
        )
        endControlFlow()
        addStatement(".also { reader.endObject() }")

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

private fun CodeGenerationAst.FieldType.fromResponseCode(fieldName: String): CodeBlock {
  val builder = CodeBlock.builder()
  builder.add("${fieldName.escapeKotlinReservedWord()}Adapter.fromResponse(reader)")
  if (!nullable) {
    builder.add(" ?: throw %T(%S)", UnexpectedNullValue::class.asTypeName(), fieldName)
  }
  return builder.build()
}

