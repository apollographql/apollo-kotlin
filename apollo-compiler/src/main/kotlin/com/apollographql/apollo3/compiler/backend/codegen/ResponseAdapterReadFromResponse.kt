package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.internal.json.MapJsonReader
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.responseAdapterCache
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.ObjectType.readFromResponseFunSpec(generateFragmentsAsInterfaces: Boolean): FunSpec {
  return when (this.kind) {
    is CodeGenerationAst.ObjectType.Kind.ObjectWithFragments -> {
      if (generateFragmentsAsInterfaces) readFragmentFromStreamResponseFunSpec() else readFragmentFromBufferedResponseFunSpec()
    }
    is CodeGenerationAst.ObjectType.Kind.FragmentDelegate -> readFragmentDelegateFromResponseFunSpec()
    else -> readObjectFromResponseFunSpec()
  }
}

private fun CodeGenerationAst.ObjectType.readObjectFromResponseFunSpec(): FunSpec {
  val fieldVariablesCode = this.fieldVariablesCode()
  val selectFieldsCode = this.selectFieldsCode()

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
      .returns(this.typeRef.asTypeName())
      .addParameter("reader", JsonReader::class)
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .apply {
        if (isTypeCase) {
          addParameter(CodeGenerationAst.typenameField.asOptionalParameterSpec(withDefaultValue = false))
        } else {
          addModifiers(KModifier.OVERRIDE)
        }
      }
      .addCode(CodeBlock
          .builder()
          .add(fieldVariablesCode)
          .addStatement("")
          .applyIf(!isTypeCase) { addStatement("reader.beginObject()") }
          .add(selectFieldsCode)
          .applyIf(!isTypeCase) { addStatement("reader.endObject()") }
          .add("return·%L", typeConstructorCode)
          .build()
      )
      .build()
}

private fun CodeGenerationAst.ObjectType.fieldVariablesCode(): CodeBlock {
  return this.fields
      .map { field ->
        if (field.responseName == CodeGenerationAst.typenameField.responseName) {
          CodeBlock.of(
              "var·%L:·%T·=·%L",
              field.name.escapeKotlinReservedWord(),
              field.type.asTypeName().copy(nullable = true),
              if (isTypeCase) "__typename" else "null"
          )
        } else {
          CodeBlock.of(
              "var·%L:·%T·=·null",
              field.name.escapeKotlinReservedWord(),
              field.type.asTypeName().copy(nullable = true)
          )
        }
      }
      .joinToCode(separator = "\n")
}

private fun CodeGenerationAst.ObjectType.selectFieldsCode(): CodeBlock {
  return CodeBlock.builder()
      .beginControlFlow("while(true)")
      .beginControlFlow("when·(reader.selectName(RESPONSE_NAMES))")
      .add(
          this.fields.mapIndexed { fieldIndex, field ->
            CodeBlock.of(
                "%L·->·%L·=·%L",
                fieldIndex,
                field.name.escapeKotlinReservedWord(),
                field.type.fromResponseCode()
            )
          }.joinToCode(separator = "\n", suffix = "\n")
      )
      .addStatement("else -> break")
      .endControlFlow()
      .endControlFlow()
      .build()
}

private fun CodeGenerationAst.ObjectType.readFragmentFromStreamResponseFunSpec(): FunSpec {
  val (defaultImplementation, possibleImplementations) = with(this.kind as CodeGenerationAst.ObjectType.Kind.ObjectWithFragments) {
    defaultImplementation to possibleImplementations
  }
  return fromResponseFunSpecBuilder()
      .returns(this.typeRef.asTypeName())
      .applyIf(possibleImplementations.isEmpty()) {
        addStatement(
            "return·%T.fromResponse(reader)",
            defaultImplementation!!.asAdapterTypeName()
        )
      }
      .applyIf(possibleImplementations.isNotEmpty()) {
        addStatement("reader.beginObject()")
        addStatement("check(reader.nextName() == \"__typename\")")
        addStatement("val·typename·=·reader.nextString()")
        beginControlFlow("return·when(typename)")
        addCode(
            possibleImplementations
                .flatMap { fragmentImplementation ->
                  fragmentImplementation.typeConditions.map { typeCondition ->
                    CodeBlock.of(
                        "%S·->·%T.fromResponse(reader,·$responseAdapterCache,·typename)",
                        typeCondition,
                        fragmentImplementation.typeRef.asAdapterTypeName(),
                    )
                  }
                }
                .joinToCode(separator = "\n", suffix = "\n")
        )
        addStatement(
            "else·->·%T.fromResponse(reader,·$responseAdapterCache,·typename)",
            defaultImplementation!!.asAdapterTypeName()
        )
        endControlFlow()
        addStatement(".also { reader.endObject() }")

      }
      .build()
}

private fun CodeGenerationAst.ObjectType.readFragmentFromBufferedResponseFunSpec(): FunSpec {
  val possibleImplementations = (this.kind as CodeGenerationAst.ObjectType.Kind.ObjectWithFragments).possibleImplementations

  val fragmentVariablesCode = possibleImplementations.map { fragmentImplementation ->
    CodeBlock.of(
        "var·%L:·%T·=·null",
        fragmentImplementation.typeRef.fragmentPropertyName(),
        fragmentImplementation.typeRef.asTypeName().copy(nullable = true)
    )
  }.joinToCode(separator = "\n", suffix = "\n")

  val readFragmentsCode = possibleImplementations.map { fragmentImplementation ->
    val possibleTypenamesArray = fragmentImplementation.typeConditions
        .map { CodeBlock.of("%S", it) }
        .joinToCode(separator = ", ")
    CodeBlock.builder()
        .beginControlFlow("if·(__typename·in·arrayOf(%L))", possibleTypenamesArray)
        .addStatement("reader.rewind()")
        .run {
          if (fragmentImplementation.typeRef.isNamedFragmentDataRef) {
            addStatement(
                "%L·=·%T.fromResponse(reader,·$responseAdapterCache)",
                fragmentImplementation.typeRef.fragmentPropertyName(),
                fragmentImplementation.typeRef.enclosingType!!.asAdapterTypeName()
            )
          } else {
            addStatement(
                "%L·=·%T.fromResponse(reader,·$responseAdapterCache,·__typename)",
                fragmentImplementation.typeRef.fragmentPropertyName(),
                fragmentImplementation.typeRef.asAdapterTypeName()
            )
          }
        }
        .endControlFlow()
        .build()
  }.joinToCode(separator = "", suffix = "\n")

  val typeConstructorCode = CodeBlock.builder()
      .addStatement("return·%T(", this.typeRef.asTypeName())
      .indent()
      .add(
          this.fields.map { field ->
            CodeBlock.of(
                "%L·=·%L%L",
                field.name.escapeKotlinReservedWord(),
                field.name.escapeKotlinReservedWord(),
                "!!".takeUnless { field.type.nullable } ?: ""
            )
          }.joinToCode(separator = ",\n", suffix = ",\n")
      )
      .add(
          possibleImplementations.map { fragmentImplementation ->
            val propertyName = fragmentImplementation.typeRef.fragmentPropertyName()
            CodeBlock.of("%L·=·%L", propertyName, propertyName)
          }.joinToCode(separator = ",\n", suffix = "\n")
      )
      .unindent()
      .addStatement(")")
      .build()

  return FunSpec.builder("fromResponse")
      .addModifiers(KModifier.OVERRIDE)
      .returns(this.typeRef.asTypeName())
      .addParameter(ParameterSpec.builder("reader", JsonReader::class).build())
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addStatement("val reader = reader.%M()", MemberName(MapJsonReader.Companion::class.asClassName(), "buffer"))
      .addStatement("reader.beginObject()\n")
      .addCode(this.fieldVariablesCode())
      .addStatement("")
      .addCode(fragmentVariablesCode)
      .addCode(this.selectFieldsCode())
      .addCode(readFragmentsCode)
      .addCode(typeConstructorCode)
      .addStatement(".also { reader.endObject() }")
      .build()
}

private fun CodeGenerationAst.ObjectType.readFragmentDelegateFromResponseFunSpec(): FunSpec {
  val fragmentRef = (this.kind as CodeGenerationAst.ObjectType.Kind.FragmentDelegate).fragmentTypeRef
  return fromResponseFunSpecBuilder()
      .addParameter(CodeGenerationAst.typenameField.asOptionalParameterSpec(withDefaultValue = false))
      .returns(this.typeRef.asTypeName())
      .addStatement(
          "return·%T(%T.fromResponse(reader,·%L))",
          this.typeRef.asTypeName(),
          fragmentRef.enclosingType!!.asAdapterTypeName(),
          CodeGenerationAst.typenameField.responseName.escapeKotlinReservedWord()
      )
      .build()
}

private fun CodeGenerationAst.FieldType.fromResponseCode(): CodeBlock {
  val builder = CodeBlock.builder()
  builder.add("%L.fromResponse(reader, $responseAdapterCache)", adapterInitializer(this))
  return builder.build()
}

internal fun fromResponseFunSpecBuilder() = FunSpec.builder("fromResponse")
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(ParameterSpec.builder("reader", JsonReader::class).build())
    .addParameter(responseAdapterCache, ResponseAdapterCache::class)

