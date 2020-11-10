package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.OperationRequestBodyComposer
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.SimpleOperationResponseParser
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.InputType
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asPropertySpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asTypeName
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.createMapperFun
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.marshallerFunSpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.responseFieldsPropertySpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.suppressWarningsAnnotation
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.toDefaultValueCodeBlock
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.toMapperFun
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode
import okio.Buffer
import okio.BufferedSource
import okio.ByteString

private val DEFAULT_SCALAR_TYPE_ADAPTERS = MemberName(ScalarTypeAdapters.Companion::class.asClassName(), "DEFAULT")

internal fun OperationType.typeSpec(targetPackage: String, generateAsInternal: Boolean = false) = TypeSpec
    .classBuilder(name)
    .addAnnotation(suppressWarningsAnnotation)
    .addSuperinterface(superInterfaceType(targetPackage))
    .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
    .applyIf(description.isNotBlank()) { addKdoc("%L", description) }
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
        .beginControlFlow("return %T.invoke·{", ResponseFieldMapper::class)
        .addStatement("%T(it)", data.asTypeName())
        .endControlFlow()
        .build()
    )
    .addFunction(parseWithAdaptersFunSpec())
    .addFunction(parseByteStringWithAdaptersFunSpec())
    .addFunction(parseFunSpec())
    .addFunction(parseByteStringFunSpec())
    .addFunction(composeRequestBodyFunSpec())
    .addFunction(composeRequestBodyWithDefaultAdaptersFunSpec())
    .addFunction(composeRequestBodyFunSpecForQuery())
    .addTypes(nestedObjects.map { (ref, type) ->
      if (ref == data) {
        type.toOperationDataTypeSpec(name = data.name, generateAsInternal = generateAsInternal)
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
        .addProperty(PropertySpec
            .builder("OPERATION_NAME", OperationName::class)
            .initializer("%L", TypeSpec.anonymousClassBuilder()
                .addSuperinterface(OperationName::class)
                .addFunction(FunSpec
                    .builder("name")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(String::class)
                    .addStatement("return %S", operationName)
                    .build()
                )
                .build()
            )
            .build()
        )
        .build()
    )
    .build()

private fun OperationType.superInterfaceType(targetPackage: String): TypeName {
  val dataTypeName = ClassName(targetPackage, name, "Data")
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
          val typeName = variable.type.asTypeName().let {
            if (variable.isOptional) Input::class.asClassName().parameterizedBy(it) else it
          }

          ParameterSpec
              .builder(
                  name = variable.name,
                  type = typeName
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
              CodeBlock.builder()
                  .addStatement("if·(this@%L.%L.defined)·{", operationType.name, field.name)
                  .indent()
                  .addStatement("this[%S]·=·this@%L.%L.value", field.schemaName, operationType.name, field.name)
                  .unindent()
                  .addStatement("}")
                  .build()
            } else {
              CodeBlock.of("this[%S]·=·this@%L.%L\n", field.schemaName, operationType.name, field.name)
            }
          }.joinToCode(separator = "")
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
          .beginControlFlow("return %T.invoke·{ writer ->", InputFieldMarshaller::class)
          .apply { fields.forEach { field -> add(field.writeCodeBlock(thisRef)) } }
          .endControlFlow()
          .build()
      )
      .build()
}

private fun ObjectType.toOperationDataTypeSpec(name: String, generateAsInternal: Boolean) =
    TypeSpec
        .classBuilder(name)
        .addModifiers(KModifier.DATA)
        .addSuperinterface(Operation.Data::class)
        .apply { if (description.isNotBlank()) addKdoc("%L\n", description) }
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
            .addFunction(fields.toMapperFun(ClassName("", name)))
            .addFunction(ClassName("", name).createMapperFun())
            .build()
        )
        .addFunction(fields.marshallerFunSpec(override = true, thisRef = name))
        .applyIf(fragmentsType != null) { addType(fragmentsType!!.fragmentsTypeSpec(generateAsInternal)) }
        .build()

private fun OperationType.parseWithAdaptersFunSpec() = FunSpec.builder("parse")
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(ParameterSpec("source", BufferedSource::class.asTypeName()))
    .addParameter(ParameterSpec("scalarTypeAdapters", ScalarTypeAdapters::class.asTypeName()))
    .throwsMultiplatformIOException()
    .returns(responseReturnType())
    .addStatement("return %T.parse(source, this, scalarTypeAdapters)", SimpleOperationResponseParser::class)
    .build()

private fun OperationType.parseByteStringWithAdaptersFunSpec() = FunSpec.builder("parse")
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(ParameterSpec("byteString", ByteString::class.asTypeName()))
    .addParameter(ParameterSpec("scalarTypeAdapters", ScalarTypeAdapters::class.asTypeName()))
    .throwsMultiplatformIOException()
    .returns(responseReturnType())
    .addStatement("return parse(%T().write(byteString), scalarTypeAdapters)", Buffer::class)
    .build()

private fun OperationType.parseFunSpec() = FunSpec.builder("parse")
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(ParameterSpec("source", BufferedSource::class.asTypeName()))
    .throwsMultiplatformIOException()
    .returns(responseReturnType())
    .addStatement("return parse(source, %M)", DEFAULT_SCALAR_TYPE_ADAPTERS)
    .build()

private fun OperationType.parseByteStringFunSpec() = FunSpec.builder("parse")
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(ParameterSpec("byteString", ByteString::class.asTypeName()))
    .throwsMultiplatformIOException()
    .returns(responseReturnType())
    .addStatement("return parse(byteString, %M)", DEFAULT_SCALAR_TYPE_ADAPTERS)
    .build()

private fun OperationType.responseReturnType() = Response::class.asClassName().parameterizedBy(data.asTypeName())

private fun composeRequestBodyFunSpec(): FunSpec {
  return FunSpec.builder("composeRequestBody")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec("scalarTypeAdapters", ScalarTypeAdapters::class.asTypeName()))
      .returns(ByteString::class)
      .addCode(
          CodeBlock.builder()
              .add("return %T.compose(\n", OperationRequestBodyComposer::class)
              .indent()
              .addStatement("operation = this,")
              .addStatement("autoPersistQueries = false,")
              .addStatement("withQueryDocument = true,")
              .addStatement("scalarTypeAdapters = scalarTypeAdapters")
              .unindent()
              .add(")\n")
              .build()
      )
      .build()
}

private fun composeRequestBodyWithDefaultAdaptersFunSpec(): FunSpec {
  return FunSpec.builder("composeRequestBody")
      .addModifiers(KModifier.OVERRIDE)
      .returns(ByteString::class)
      .addCode(
          CodeBlock.builder()
              .add("return %T.compose(\n", OperationRequestBodyComposer::class)
              .indent()
              .addStatement("operation = this,")
              .addStatement("autoPersistQueries = false,")
              .addStatement("withQueryDocument = true,")
              .addStatement("scalarTypeAdapters = %M", DEFAULT_SCALAR_TYPE_ADAPTERS)
              .unindent()
              .add(")\n")
              .build()
      )
      .build()
}

private fun composeRequestBodyFunSpecForQuery(): FunSpec {
  return FunSpec.builder("composeRequestBody")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec("autoPersistQueries", Boolean::class.asTypeName()))
      .addParameter(ParameterSpec("withQueryDocument", Boolean::class.asTypeName()))
      .addParameter(ParameterSpec("scalarTypeAdapters", ScalarTypeAdapters::class.asTypeName()))
      .returns(ByteString::class)
      .addCode(
          CodeBlock.builder()
              .add("return %T.compose(\n", OperationRequestBodyComposer::class)
              .indent()
              .addStatement("operation = this,")
              .addStatement("autoPersistQueries = autoPersistQueries,")
              .addStatement("withQueryDocument = withQueryDocument,")
              .addStatement("scalarTypeAdapters = scalarTypeAdapters")
              .unindent()
              .add(")\n")
              .build()
      )
      .build()
}
