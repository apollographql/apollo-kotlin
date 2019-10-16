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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal fun ObjectType.typeSpec(): TypeSpec {
  return when (this) {
    is ObjectType.Object -> TypeSpec
        .classBuilder(className)
        .addModifiers(KModifier.DATA)
        .primaryConstructor(primaryConstructorSpec)
        .addProperties(fields.map { it.asPropertySpec(initializer = CodeBlock.of(it.name)) })
        .addType(companionObjectSpec)
        .applyIf(fragmentsType != null) { addType(fragmentsType!!.fragmentsTypeSpec) }
        .addFunction(marshallerFunSpec(fields))
        .build()

    is ObjectType.InlineFragmentSuper -> TypeSpec
        .interfaceBuilder(className)
        .addFunction(
            FunSpec.builder("marshaller")
                .addModifiers(KModifier.ABSTRACT)
                .returns(ResponseFieldMarshaller::class)
                .build()
        )
        .build()

    is ObjectType.InlineFragment -> TypeSpec
        .classBuilder(className)
        .addModifiers(KModifier.DATA)
        .primaryConstructor(primaryConstructorSpec)
        .addProperties(fields.map { it.asPropertySpec(initializer = CodeBlock.of(it.name)) })
        .addSuperinterface(superInterface.asTypeName())
        .addType(companionObjectSpec)
        .addFunction(marshallerFunSpec(fields = fields, override = true))
        .build()
  }
}

internal val ObjectType.fragmentsTypeSpec: TypeSpec
  get() {
    return TypeSpec.classBuilder(className)
        .addModifiers(KModifier.DATA)
        .primaryConstructor(primaryConstructorSpec)
        .addProperties(fields.map { field -> field.asPropertySpec(initializer = CodeBlock.of(field.name)) })
        .addFunction(marshallerFunSpec)
        .build()
  }

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
        .addFunction(fields.toMapperFun(ClassName(packageName = "", simpleName = className)))
        .build()
  }

private val ObjectType.InlineFragment.companionObjectSpec: TypeSpec
  get() {
    return TypeSpec.companionObjectBuilder()
        .addProperty(responseFieldsPropertySpec(fields))
        .addFunction(fields.toMapperFun(ClassName.bestGuess(className)))
        .addProperty(PropertySpec.builder("POSSIBLE_TYPES",
            Array<String>::class.asClassName().parameterizedBy(String::class.asClassName()))
            .initializer(possibleTypes
                .map { CodeBlock.of("%S", it) }
                .joinToCode(prefix = "arrayOf(", separator = ", ", suffix = ")")
            )
            .build()
        )
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
