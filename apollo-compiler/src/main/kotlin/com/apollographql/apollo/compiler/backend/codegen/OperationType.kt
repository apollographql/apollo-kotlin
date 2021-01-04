package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.OperationRequestBodyComposer
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.SimpleOperationResponseParser
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
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

private val DEFAULT_CUSTOM_SCALAR_ADAPTERS = MemberName(CustomScalarAdapters.Companion::class.asClassName(), "DEFAULT")

internal fun CodeGenerationAst.OperationType.typeSpec(targetPackage: String, generateAsInternal: Boolean = false): TypeSpec {
  val operationResponseAdapter = CodeGenerationAst.TypeRef(name = name, packageName = targetPackage).asAdapterTypeName()
  return TypeSpec
      .classBuilder(name.escapeKotlinReservedWord())
      .addAnnotation(suppressWarningsAnnotation)
      .addSuperinterface(superInterfaceType(targetPackage))
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .applyIf(description.isNotBlank()) { addKdoc("%L", description) }
      .applyIf(variables.isNotEmpty()) {
        addModifiers(KModifier.DATA)
        primaryConstructor(primaryConstructorSpec)
        addProperties(variables.map { variable -> variable.asPropertySpec(CodeBlock.of(variable.name.escapeKotlinReservedWord())) })
        addProperty(variablePropertySpec)
      }
      .addFunction(FunSpec.builder("operationId")
          .addModifiers(KModifier.OVERRIDE)
          .returns(String::class)
          .addStatement("return OPERATION_ID")
          .build()
      )
      .addFunction(FunSpec.builder("queryDocument")
          .addModifiers(KModifier.OVERRIDE)
          .returns(String::class)
          .addStatement("return QUERY_DOCUMENT")
          .build()
      )
      .addFunction(FunSpec.builder("variables")
          .addModifiers(KModifier.OVERRIDE)
          .returns(Operation.Variables::class.asClassName())
          .apply {
            if (variables.isNotEmpty()) {
              addStatement("return variables")
            } else {
              addStatement("return %T.EMPTY_VARIABLES", Operation::class)
            }
          }
          .build()
      )
      .addFunction(FunSpec.builder("name")
          .addModifiers(KModifier.OVERRIDE)
          .returns(OperationName::class)
          .addStatement("return OPERATION_NAME")
          .build()
      )
      .addFunction(
          FunSpec.builder("responseFieldMapper")
              .addModifiers(KModifier.OVERRIDE)
              .returns(ResponseFieldMapper::class.asClassName().parameterizedBy(ClassName(packageName = "", "Data")))
              .beginControlFlow("return·%T·{·reader·->", ResponseFieldMapper::class)
              .addStatement("%T.fromResponse(reader)", operationResponseAdapter)
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
      .addType(this.dataType.typeSpec())
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
                      .addStatement("return·%S", operationName)
                      .build()
                  )
                  .build()
              )
              .build()
          )
          .build()
      )
      .build()
}

private fun CodeGenerationAst.OperationType.superInterfaceType(targetPackage: String): TypeName {
  val dataTypeName = ClassName(targetPackage, name.escapeKotlinReservedWord(), "Data")
  return when (type) {
    CodeGenerationAst.OperationType.Type.QUERY -> Query::class.asClassName()
    CodeGenerationAst.OperationType.Type.MUTATION -> Mutation::class.asClassName()
    CodeGenerationAst.OperationType.Type.SUBSCRIPTION -> Subscription::class.asClassName()
  }.parameterizedBy(dataTypeName, Operation.Variables::class.asClassName())
}

private val CodeGenerationAst.OperationType.primaryConstructorSpec: FunSpec
  get() {
    return FunSpec
        .constructorBuilder()
        .addParameters(variables.map { variable ->
          ParameterSpec
              .builder(
                  name = variable.name.escapeKotlinReservedWord(),
                  type = variable.type.asTypeName().let { type ->
                    if (type.isNullable) Input::class.asClassName().parameterizedBy(type.copy(nullable = false)) else type
                  }
              )
              .applyIf(variable.type.nullable) { defaultValue("%T.absent()", Input::class.asClassName()) }
              .build()
        })
        .build()
  }

private val CodeGenerationAst.OperationType.variablePropertySpec: PropertySpec
  get() {
    return PropertySpec
        .builder("variables", Operation.Variables::class)
        .addModifiers(KModifier.PRIVATE)
        .addAnnotation(Transient::class)
        .initializer("%L", TypeSpec.anonymousClassBuilder()
            .superclass(Operation.Variables::class)
            .addFunction(variables.variablesValueMapSpec(this))
            .addFunction(variables.variablesMarshallerSpec(name.escapeKotlinReservedWord()))
            .build()
        )
        .build()
  }

private fun List<CodeGenerationAst.InputField>.variablesValueMapSpec(operationType: CodeGenerationAst.OperationType): FunSpec {
  return FunSpec
      .builder("valueMap")
      .addModifiers(KModifier.OVERRIDE)
      .returns(Map::class.asClassName().parameterizedBy(String::class.asClassName(), Any::class.asClassName().copy(nullable = true)))
      .beginControlFlow("return mutableMapOf<%T, %T>().apply", String::class, Any::class.asClassName().copy(nullable = true))
      .addCode(
          map { field ->
            if (field.type.nullable) {
              CodeBlock.builder()
                  .addStatement(
                      "if·(this@%L.%L.defined)·{",
                      operationType.name.escapeKotlinReservedWord(),
                      field.name.escapeKotlinReservedWord()
                  )
                  .indent()
                  .addStatement(
                      "this[%S]·=·this@%L.%L.value",
                      field.schemaName,
                      operationType.name.escapeKotlinReservedWord(),
                      field.name.escapeKotlinReservedWord()
                  )
                  .unindent()
                  .addStatement("}")
                  .build()
            } else {
              CodeBlock.of(
                  "this[%S]·=·this@%L.%L\n",
                  field.schemaName,
                  operationType.name.escapeKotlinReservedWord(),
                  field.name.escapeKotlinReservedWord()
              )
            }
          }.joinToCode(separator = "")
      )
      .endControlFlow()
      .build()
}

private fun List<CodeGenerationAst.InputField>.variablesMarshallerSpec(thisRef: String): FunSpec {
  return FunSpec
      .builder("marshaller")
      .returns(InputFieldMarshaller::class)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(CodeBlock
          .builder()
          .beginControlFlow("return·%T.invoke·{ writer ->", InputFieldMarshaller::class)
          .apply { forEach { field -> add(field.writeCodeBlock(thisRef)) } }
          .endControlFlow()
          .build()
      )
      .build()
}

private fun CodeGenerationAst.OperationType.parseWithAdaptersFunSpec(): FunSpec {
  return FunSpec.builder("parse")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec("source", BufferedSource::class.asTypeName()))
      .addParameter(ParameterSpec("customScalarAdapters", CustomScalarAdapters::class.asTypeName()))
      .throwsMultiplatformIOException()
      .returns(responseReturnType())
      .addStatement("return·%T.parse(source,·this,·customScalarAdapters)", SimpleOperationResponseParser::class)
      .build()
}

private fun CodeGenerationAst.OperationType.parseByteStringWithAdaptersFunSpec(): FunSpec {
  return FunSpec.builder("parse")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec("byteString", ByteString::class.asTypeName()))
      .addParameter(ParameterSpec("customScalarAdapters", CustomScalarAdapters::class.asTypeName()))
      .throwsMultiplatformIOException()
      .returns(responseReturnType())
      .addStatement("return·parse(%T().write(byteString),·customScalarAdapters)", Buffer::class)
      .build()
}

private fun CodeGenerationAst.OperationType.parseFunSpec(): FunSpec {
  return FunSpec.builder("parse")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec("source", BufferedSource::class.asTypeName()))
      .throwsMultiplatformIOException()
      .returns(responseReturnType())
      .addStatement("return·parse(source,·%M)", DEFAULT_CUSTOM_SCALAR_ADAPTERS)
      .build()
}

private fun CodeGenerationAst.OperationType.parseByteStringFunSpec(): FunSpec {
  return FunSpec.builder("parse")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec("byteString", ByteString::class.asTypeName()))
      .throwsMultiplatformIOException()
      .returns(responseReturnType())
      .addStatement("return·parse(byteString,·%M)", DEFAULT_CUSTOM_SCALAR_ADAPTERS)
      .build()
}

private fun CodeGenerationAst.OperationType.responseReturnType(): TypeName {
  return Response::class.asClassName().parameterizedBy(ClassName(packageName = "", "Data"))
}

private fun composeRequestBodyFunSpec(): FunSpec {
  return FunSpec.builder("composeRequestBody")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(ParameterSpec("customScalarAdapters", CustomScalarAdapters::class.asTypeName()))
      .returns(ByteString::class)
      .addCode(
          CodeBlock.builder()
              .add("return·%T.compose(\n", OperationRequestBodyComposer::class)
              .indent()
              .addStatement("operation = this,")
              .addStatement("autoPersistQueries = false,")
              .addStatement("withQueryDocument = true,")
              .addStatement("customScalarAdapters = customScalarAdapters")
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
              .addStatement("customScalarAdapters = %M", DEFAULT_CUSTOM_SCALAR_ADAPTERS)
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
      .addParameter(ParameterSpec("customScalarAdapters", CustomScalarAdapters::class.asTypeName()))
      .returns(ByteString::class)
      .addCode(
          CodeBlock.builder()
              .add("return %T.compose(\n", OperationRequestBodyComposer::class)
              .indent()
              .addStatement("operation = this,")
              .addStatement("autoPersistQueries = autoPersistQueries,")
              .addStatement("withQueryDocument = withQueryDocument,")
              .addStatement("customScalarAdapters = customScalarAdapters")
              .unindent()
              .add(")\n")
              .build()
      )
      .build()
}
