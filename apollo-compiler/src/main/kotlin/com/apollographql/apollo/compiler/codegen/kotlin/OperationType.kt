package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.*
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.SimpleOperationResponseParser
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.InputType
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asPropertySpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asTypeName
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.marshallerFunSpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.responseFieldsPropertySpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.suppressWarningsAnnotation
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.toMapperFun
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.throws
import okio.BufferedSource
import java.io.IOException

internal fun OperationType.typeSpec(targetPackage: String, generateAsInternal: Boolean = false) = TypeSpec
    .classBuilder(name)
    .addAnnotation(suppressWarningsAnnotation)
    .addSuperinterface(superInterfaceType(targetPackage))
    .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
    .applyIf(variables.fields.isNotEmpty()) {
      addModifiers(KModifier.DATA)
      primaryConstructor(primaryConstructorSpec)
      addProperties(variables.fields.map { variable -> variable.asPropertySpec(CodeBlock.of(variable.name)) })
      addProperty(variablePropertySpec)
    }
    .addFunction(FunSpec.builder("operationId")
        .addModifiers(KModifier.OVERRIDE)
        .returns(String::class)
        .addCode("return OPERATION_ID")
        .build()
    )
    .addFunction(FunSpec.builder("queryDocument")
        .addModifiers(KModifier.OVERRIDE)
        .returns(String::class)
        .addCode("return QUERY_DOCUMENT")
        .build()
    )
    .addFunction(FunSpec.builder("wrapData")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(ParameterSpec.builder("data", data.asTypeName().copy(nullable = true)).build())
        .returns(data.asTypeName().copy(nullable = true))
        .addCode("return data")
        .build()
    )
    .addFunction(FunSpec.builder("variables")
        .addModifiers(KModifier.OVERRIDE)
        .returns(Operation.Variables::class.asClassName())
        .apply {
          if (variables.fields.isNotEmpty()) {
            addCode("return variables")
          } else {
            addCode("return %T.EMPTY_VARIABLES", Operation::class)
          }
        }
        .build()
    )
    .addFunction(FunSpec.builder("name")
        .addModifiers(KModifier.OVERRIDE)
        .returns(OperationName::class)
        .addCode("return OPERATION_NAME")
        .build()
    )
    .addFunction(FunSpec.builder("responseFieldMapper")
        .addModifiers(KModifier.OVERRIDE)
        .returns(ResponseFieldMapper::class.asClassName().parameterizedBy(data.asTypeName()))
        .beginControlFlow("return %T {", ResponseFieldMapper::class)
        .addStatement("%T(it)", data.asTypeName())
        .endControlFlow()
        .build()
    )
    .addFunction(FunSpec.builder("parse")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(ParameterSpec
            .builder("source", BufferedSource::class)
            .build()
        )
        .addParameter(ParameterSpec
            .builder("scalarTypeAdapters", ScalarTypeAdapters::class)
            .build()
        )
        .throws(IOException::class)
        .returns(Response::class.asClassName().parameterizedBy(data.asTypeName()))
        .addStatement("return %T.parse(source, this, scalarTypeAdapters)", SimpleOperationResponseParser::class)
        .build()
    )
    .addFunction(FunSpec.builder("parse")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(ParameterSpec
            .builder("source", BufferedSource::class)
            .build()
        )
        .throws(IOException::class)
        .returns(Response::class.asClassName().parameterizedBy(data.asTypeName()))
        .addStatement("return parse(source, %M)", MemberName(ScalarTypeAdapters::class.asClassName(), "DEFAULT"))
        .build()
    )
    .addTypes(nestedObjects.map { (ref, type) ->
      if (ref == data) {
        type.toOperationDataTypeSpec(data.name)
      } else {
        type.typeSpec()
      }
    })
    .addType(TypeSpec.companionObjectBuilder()
        .addProperty(PropertySpec.builder("OPERATION_ID", String::class)
            .addModifiers(KModifier.CONST)
            .initializer("%S", operationId)
            .build()
        )
        .addProperty(PropertySpec.builder("QUERY_DOCUMENT", String::class)
            .initializer(
                CodeBlock.builder()
                    .add("%T.minify(\n", QueryDocumentMinifier::class.java)
                    .indent()
                    .add("%S\n", queryDocument)
                    .unindent()
                    .add(")")
                    .build()
            )
            .build()
        )
        .addProperty(PropertySpec.builder("OPERATION_NAME", OperationName::class)
            .initializer("%T { %S }", OperationName::class, operationName)
            .build())
        .build()
    )
    .build()

private fun OperationType.superInterfaceType(targetPackage: String): TypeName {
  val dataTypeName = ClassName(packageName = targetPackage, simpleName = name, simpleNames = *arrayOf("Data"))
  return when (type) {
    OperationType.Type.QUERY -> Query::class.asClassName()
    OperationType.Type.MUTATION -> Mutation::class.asClassName()
    OperationType.Type.SUBSCRIPTION -> Subscription::class.asClassName()
  }.parameterizedBy(dataTypeName, dataTypeName, Operation.Variables::class.asClassName())
}

private val OperationType.primaryConstructorSpec: FunSpec
  get() {
    return FunSpec
        .constructorBuilder()
        .addParameters(variables.fields.map { variable ->
          val typeName = variable.type.asTypeName()
          ParameterSpec
              .builder(
                  name = variable.name,
                  type = if (variable.isOptional) Input::class.asClassName().parameterizedBy(typeName) else typeName
              )
              .applyIf(variable.isOptional) { defaultValue("%T.absent()", Input::class.asClassName()) }
              .build()
        })
        .build()
  }

private val OperationType.variablePropertySpec: PropertySpec
  get() {
    return PropertySpec
        .builder("variables", Operation.Variables::class)
        .addModifiers(KModifier.PRIVATE)
        .addAnnotation(Transient::class)
        .initializer("%L", TypeSpec.anonymousClassBuilder()
            .superclass(Operation.Variables::class)
            .addFunction(variables.variablesValueMapSpec(this))
            .addFunction(variables.variablesMarshallerSpec(name))
            .build()
        )
        .build()
  }

private fun InputType.variablesValueMapSpec(operationType: OperationType): FunSpec {
  return FunSpec
      .builder("valueMap")
      .addModifiers(KModifier.OVERRIDE)
      .returns(Map::class.asClassName().parameterizedBy(String::class.asClassName(), Any::class.asClassName().copy(nullable = true)))
      .beginControlFlow("return mutableMapOf<%T, %T>().apply", String::class, Any::class.asClassName().copy(nullable = true))
      .addCode(
          fields.map { field ->
            if (field.isOptional) {
              CodeBlock.of("if (this@%L.%L.defined) this[%S] = this@%L.%L.value", operationType.name, field.name, field.schemaName, operationType.name, field.name)
            } else {
              CodeBlock.of("this[%S] = this@%L.%L", field.schemaName, operationType.name, field.name)
            }
          }.joinToCode(separator = "\n", suffix = "\n")
      )
      .endControlFlow()
      .build()
}

private fun InputType.variablesMarshallerSpec(thisRef: String): FunSpec {
  return FunSpec
      .builder("marshaller")
      .returns(InputFieldMarshaller::class)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(CodeBlock
          .builder()
          .beginControlFlow("return %T { writer ->", InputFieldMarshaller::class)
          .apply { fields.forEach { field -> add(field.writeCodeBlock(thisRef)) } }
          .endControlFlow()
          .build()
      )
      .build()
}

private fun ObjectType.toOperationDataTypeSpec(name: String) =
    TypeSpec
        .classBuilder(name)
        .addModifiers(KModifier.DATA)
        .addSuperinterface(Operation.Data::class)
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameters(fields.map { field ->
              val typeName = field.type.asTypeName()
              ParameterSpec.builder(
                  name = field.name,
                  type = if (field.isOptional) typeName.copy(nullable = true) else typeName
              ).build()
            })
            .build()
        )
        .addProperties(fields.map { field -> field.asPropertySpec(initializer = CodeBlock.of(field.name)) })
        .addType(TypeSpec.companionObjectBuilder()
            .addProperty(responseFieldsPropertySpec(fields))
            .addFunction(fields.toMapperFun(ClassName.bestGuess(name)))
            .build()
        )
        .addFunction(fields.marshallerFunSpec(override = true, thisRef = name))
        .build()
