package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asPropertySpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asTypeName
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.marshallerFunSpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.responseFieldsPropertySpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.toMapperFun
import com.squareup.kotlinpoet.*

internal fun ObjectType.typeSpec() =
    TypeSpec
        .classBuilder(className)
        .addModifiers(KModifier.DATA)
        .primaryConstructor(primaryConstructorSpec)
        .addProperties(fields.map { it.asPropertySpec(initializer = CodeBlock.of(it.name)) })
        .addType(companionObjectSpec)
        .applyIf(fragmentsType != null) { addType(fragmentsType!!.fragmentsTypeSpec) }
        .addFunction(marshallerFunSpec(fields))
        .build()

private val ObjectType.primaryConstructorSpec: FunSpec
  get() {
    return FunSpec.constructorBuilder()
        .addParameters(fields.map { field ->
          val typeName = field.type.asTypeName()
          ParameterSpec.builder(
              name = field.name,
              type = if (field.isOptional) typeName.copy(nullable = true) else typeName
          ).build()
        })
        .build()
  }

private val ObjectType.companionObjectSpec: TypeSpec
  get() {
    return TypeSpec.companionObjectBuilder()
        .addProperty(responseFieldsPropertySpec(fields))
        .addFunction(fields.toMapperFun(ClassName.bestGuess(className)))
        .build()
  }

private val ObjectType.fragmentsTypeSpec: TypeSpec
  get() {
    return TypeSpec.classBuilder(className)
        .addModifiers(KModifier.DATA)
        .primaryConstructor(primaryConstructorSpec)
        .addProperties(fields.map { field -> field.asPropertySpec(initializer = CodeBlock.of(field.name)) })
        .addFunction(marshallerFunSpec)
        .build()
  }

private val ObjectType.marshallerFunSpec: FunSpec
  get() {
    return FunSpec.builder("marshaller")
        .returns(ResponseFieldMarshaller::class)
        .beginControlFlow("return %T", ResponseFieldMarshaller::class)
        .addCode(
            fields.map { field ->
              if (field.isOptional) {
                CodeBlock.of("%L?.marshaller()?.marshal(it)", field.name)
              } else {
                CodeBlock.of("%L.marshaller().marshal(it)", field.name)
              }
            }.joinToCode(separator = "\n", suffix = "\n")
        )
        .endControlFlow()
        .build()
  }