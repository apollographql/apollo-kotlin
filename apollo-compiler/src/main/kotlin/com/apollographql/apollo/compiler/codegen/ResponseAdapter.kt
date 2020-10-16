package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.CodeGenerationAst
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

internal fun CodeGenerationAst.OperationType.responseAdapterTypeSpec(targetPackage: String): TypeSpec {
  val dataImplementationType = checkNotNull(this.dataType.nestedTypes[this.dataType.rootType]) {
    "Failed to resolve operation root data type"
  }
  return TypeSpec.objectBuilder("${this.name}_ResponseAdapter")
      .addModifiers(KModifier.INTERNAL)
      .addAnnotation(suppressWarningsAnnotation)
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(this.dataType.rootType.asTypeName()))
      .addProperty(responseFieldsPropertySpec(dataImplementationType.fields))
      .addFunction(
          dataImplementationType.readObjectFromResponseFunSpec(
              this.dataType.rootType
                  .copy(packageName = targetPackage)
                  .asTypeName()
          )
      )
      .addTypes(
          this.dataType
              .nestedTypes
              .minus(this.dataType.rootType)
              .filterNot { (_, type) -> type.abstract && type.kind !is CodeGenerationAst.ObjectType.Kind.Fragment }
              .map { (typeRef, type) ->
                type.responseAdapterTypeSpec(
                    typeRef
                        .copy(packageName = targetPackage)
                        .asTypeName()
                )
              }
      )
      .build()
}

internal fun CodeGenerationAst.FragmentType.responseAdapterTypeSpec(targetPackage: String): TypeSpec {
  val defaultImplementationType = checkNotNull(this.nestedTypes[this.defaultImplementation]) {
    "Failed to resolve operation root data type"
  }
  return TypeSpec.objectBuilder(this.rootType.asAdapterTypeName().simpleName)
      .addModifiers(KModifier.INTERNAL)
      .addAnnotation(suppressWarningsAnnotation)
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(this.rootType.asTypeName()))
      .addProperty(responseFieldsPropertySpec(defaultImplementationType.fields))
      .addFunction(
          FunSpec.builder("fromResponse")
              .addModifiers(KModifier.OVERRIDE)
              .returns(this.rootType.asTypeName())
              .addParameter(ParameterSpec.builder("reader", ResponseReader::class).build())
              .addParameter(CodeGenerationAst.typenameField.asOptionalParameterSpec(withDefaultValue = false))
              .addStatement(
                  "return·%T.fromResponse(reader,·%L)",
                  ClassName(packageName = "", "${defaultImplementationType.name}_ResponseAdapter"),
                  CodeGenerationAst.typenameField.responseName
              )
              .build()
      )
      .addTypes(
          this.nestedTypes
              .minus(this.rootType)
              .filterNot { (_, type) -> type.abstract && type.kind !is CodeGenerationAst.ObjectType.Kind.Fragment }
              .map { (typeRef, type) ->
                type.responseAdapterTypeSpec(
                    typeRef
                        .copy(packageName = targetPackage)
                        .asTypeName()
                )
              }
      )
      .build()
}


private fun CodeGenerationAst.ObjectType.responseAdapterTypeSpec(objectTypeTypeName: TypeName): TypeSpec {
  return TypeSpec.objectBuilder("${this.name}_ResponseAdapter")
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(objectTypeTypeName))
      .applyIf(fields.isNotEmpty()) { addProperty(responseFieldsPropertySpec(fields)) }
      .addFunction(
          when (this@responseAdapterTypeSpec.kind) {
            is CodeGenerationAst.ObjectType.Kind.Fragment -> readFragmentFromResponseFunSpec(objectTypeTypeName)
            is CodeGenerationAst.ObjectType.Kind.FragmentDelegate -> readFragmentDelegateFromResponseFunSpec(objectTypeTypeName)
            else -> readObjectFromResponseFunSpec(objectTypeTypeName)
          }
      )
      .build()
}

private fun CodeGenerationAst.ObjectType.readObjectFromResponseFunSpec(objectTypeTypeName: TypeName): FunSpec {
  val fieldVariablesCode = this.fields
      .map { field ->
        if (field.responseName == CodeGenerationAst.typenameField.responseName) {
          CodeBlock.of(
              "var·%L:·%T·=·%L",
              field.name,
              field.type.asTypeName().copy(nullable = true),
              CodeGenerationAst.typenameField.responseName
          )
        } else {
          CodeBlock.of(
              "var·%L:·%T·=·null",
              field.name,
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
                field.name,
                field.type.nullable().fromResponseCode(field = "RESPONSE_FIELDS[$fieldIndex]")
            )
          }.joinToCode(separator = "\n", suffix = "\n")
      )
      .addStatement("else -> break")
      .endControlFlow()
      .endControlFlow()
      .build()

  val typeConstructorCode = CodeBlock.builder()
      .addStatement("%T(", objectTypeTypeName)
      .indent()
      .add(this.fields.map { field ->
        CodeBlock.of(
            "%L·=·%L%L",
            field.name,
            field.name,
            "!!".takeUnless { field.type.nullable } ?: ""
        )
      }.joinToCode(separator = ",\n", suffix = "\n"))
      .unindent()
      .addStatement(")")
      .build()

  return FunSpec.builder("fromResponse")
      .addModifiers(KModifier.OVERRIDE)
      .returns(objectTypeTypeName)
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

private fun CodeGenerationAst.ObjectType.readFragmentFromResponseFunSpec(objectTypeTypeName: TypeName): FunSpec {
  val (defaultImplementation, possibleImplementations) = with(this.kind as CodeGenerationAst.ObjectType.Kind.Fragment) {
    defaultImplementation to possibleImplementations
  }
  return FunSpec.builder("fromResponse")
      .addModifiers(KModifier.OVERRIDE)
      .returns(objectTypeTypeName)
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
            CodeGenerationAst.typenameField.responseName
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
            "else·->·%T.fromResponse(reader, typename)",
            defaultImplementation.asAdapterTypeName()
        )
        endControlFlow()
      }
      .build()
}

private fun CodeGenerationAst.ObjectType.readFragmentDelegateFromResponseFunSpec(objectTypeTypeName: TypeName): FunSpec {
  val fragmentRef = (this.kind as CodeGenerationAst.ObjectType.Kind.FragmentDelegate).fragmentTypeRef
  return FunSpec.builder("fromResponse")
      .addModifiers(KModifier.OVERRIDE)
      .returns(objectTypeTypeName)
      .addParameter(ParameterSpec.builder("reader", ResponseReader::class).build())
      .addParameter(CodeGenerationAst.typenameField.asOptionalParameterSpec(withDefaultValue = false))
      .addStatement(
          "return·%T(%T.fromResponse(reader,·%L))",
          objectTypeTypeName,
          fragmentRef.asAdapterTypeName(),
          CodeGenerationAst.typenameField.responseName
      )
      .build()
}

internal fun CodeGenerationAst.TypeRef.asAdapterTypeName(): ClassName {
  return if (enclosingType == null) {
    ClassName(packageName = packageName, "${this.name}_ResponseAdapter")
  } else {
    ClassName(packageName = packageName, enclosingType.asAdapterTypeName().simpleName, "${this.name}_ResponseAdapter")
  }
}


private fun CodeGenerationAst.FieldType.fromResponseCode(field: String): CodeBlock {
  val notNullOperator = "!!".takeUnless { nullable } ?: ""
  return when (this) {
    is CodeGenerationAst.FieldType.Scalar -> when (this) {
      is CodeGenerationAst.FieldType.Scalar.ID -> if (field.isNotEmpty()) {
        CodeBlock.of("readCustomType<%T>(%L·as·%T)%L", ClassName.bestGuess(type), field, ResponseField.CustomTypeField::class,
            notNullOperator)
      } else {
        CodeBlock.of("readCustomType<%T>(%T)%L", ClassName.bestGuess(type), customEnumType.asTypeName(), notNullOperator)
      }
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
        CodeBlock.of("readCustomType<%T>(%L·as·%T)%L", ClassName.bestGuess(type), field, ResponseField.CustomTypeField::class,
            notNullOperator)
      } else {
        CodeBlock.of(
            "readCustomType<%T>(%T)%L", ClassName.bestGuess(type), customEnumType.asTypeName().copy(nullable = false), notNullOperator
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
      is CodeGenerationAst.FieldType.Scalar.ID -> CodeBlock.of(
          "reader.readCustomType<%T>(%T)", ClassName.bestGuess(type), customEnumType.asTypeName()
      )
      is CodeGenerationAst.FieldType.Scalar.String -> CodeBlock.of("reader.readString()")
      is CodeGenerationAst.FieldType.Scalar.Int -> CodeBlock.of("reader.readInt()")
      is CodeGenerationAst.FieldType.Scalar.Boolean -> CodeBlock.of("reader.readBoolean()")
      is CodeGenerationAst.FieldType.Scalar.Float -> CodeBlock.of("reader.readDouble()")
      is CodeGenerationAst.FieldType.Scalar.Enum -> CodeBlock.of(
          "%T.safeValueOf(reader.readString())", typeRef.asTypeName().copy(nullable = false)
      )
      is CodeGenerationAst.FieldType.Scalar.Custom -> CodeBlock.of(
          "reader.readCustomType<%T>(%T)", ClassName.bestGuess(type), customEnumType.asTypeName()
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
