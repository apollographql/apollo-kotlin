package com.apollographql.android.compiler

import com.apollographql.android.api.graphql.ResponseFieldMapper
import com.apollographql.android.compiler.ir.*
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class OperationTypeSpecBuilder(
    val operation: Operation,
    val fragments: List<Fragment>
) : CodeGenerator {
  private val OPERATION_TYPE_NAME = operation.operationName.capitalize()
  private val OPERATION_VARIABLES_CLASS_NAME = ClassName.get("", "$OPERATION_TYPE_NAME.Variables")
  private val DATA_VAR_TYPE = ClassName.get("", "$OPERATION_TYPE_NAME.Data")

  override fun toTypeSpec(context: CodeGenerationContext): TypeSpec {
    val newContext = context.copy(reservedTypeNames = context.reservedTypeNames.plus(OPERATION_TYPE_NAME))
    return TypeSpec.classBuilder(OPERATION_TYPE_NAME)
        .addAnnotation(Annotations.GENERATED_BY_APOLLO)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addQuerySuperInterface(context, operation.variables.isNotEmpty())
        .addOperationDefinition(operation)
        .addQueryDocumentDefinition(fragments, newContext)
        .addMethod(wrapDataMethod(context))
        .addQueryConstructor(operation.variables.isNotEmpty())
        .addVariablesDefinition(operation.variables, newContext)
        .addType(operation.toTypeSpec(newContext))
        .addResponseFieldMapperMethod()
        .build()
  }

  private fun TypeSpec.Builder.addQuerySuperInterface(context: CodeGenerationContext,
      hasVariables: Boolean): TypeSpec.Builder {
    val isMutation = operation.operationType == "mutation"
    val superInterfaceClassName = if (isMutation) ClassNames.GRAPHQL_MUTATION else ClassNames.GRAPHQL_QUERY
    return if (hasVariables) {
      addSuperinterface(ParameterizedTypeName.get(superInterfaceClassName, DATA_VAR_TYPE,
          wrapperType(context), OPERATION_VARIABLES_CLASS_NAME))
    } else {
      addSuperinterface(ParameterizedTypeName.get(superInterfaceClassName, DATA_VAR_TYPE,
          wrapperType(context), ClassNames.GRAPHQL_OPERATION_VARIABLES))
    }
  }

  private fun TypeSpec.Builder.addOperationDefinition(operation: Operation): TypeSpec.Builder {
    return addField(FieldSpec.builder(ClassNames.STRING, OPERATION_DEFINITION_FIELD_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("\$S", operation.source)
        .build()
    )
  }

  private fun TypeSpec.Builder.addQueryDocumentDefinition(fragments: List<Fragment>,
      context: CodeGenerationContext): TypeSpec.Builder {
    val initializeCodeBuilder = CodeBlock.builder().add(OPERATION_DEFINITION_FIELD_NAME)
    fragments.filter { operation.fragmentsReferenced.contains(it.fragmentName) }.forEach {
      val className = ClassName.get(context.fragmentsPackage, it.interfaceTypeName());
      initializeCodeBuilder
          .add(" + \$S\n", "\n")
          .add(" + \$T.\$L", className, Fragment.FRAGMENT_DEFINITION_FIELD_NAME)
    }

    addField(FieldSpec.builder(ClassNames.STRING, QUERY_DOCUMENT_FIELD_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer(initializeCodeBuilder.build())
        .build()
    )

    addMethod(MethodSpec.methodBuilder(QUERY_DOCUMENT_ACCESSOR_NAME)
        .addAnnotation(Annotations.OVERRIDE)
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassNames.STRING)
        .addStatement("return \$L", QUERY_DOCUMENT_FIELD_NAME)
        .build()
    )

    return this
  }

  private fun wrapDataMethod(context: CodeGenerationContext): MethodSpec {
    return MethodSpec.methodBuilder("wrapData")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override::class.java)
        .addParameter(ParameterSpec.builder(DATA_VAR_TYPE, "data").build())
        .returns(wrapperType(context))
        .addStatement(
            if (context.nullableValueGenerationType != NullableValueGenerationType.ANNOTATED) {
              "return Optional.fromNullable(data)"
            } else {
              "return data"
            })
        .build()
  }

  private fun TypeSpec.Builder.addVariablesDefinition(variables: List<Variable>, context: CodeGenerationContext):
      TypeSpec.Builder {
    val queryFieldClassName =
        if (variables.isNotEmpty()) OPERATION_VARIABLES_CLASS_NAME else ClassNames.GRAPHQL_OPERATION_VARIABLES
    addField(FieldSpec.builder(queryFieldClassName, VARIABLES_FIELD_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build()
    )

    addMethod(MethodSpec.methodBuilder(VARIABLES_ACCESSOR_NAME)
        .addAnnotation(Annotations.OVERRIDE)
        .addModifiers(Modifier.PUBLIC)
        .returns(queryFieldClassName)
        .addStatement("return ${VARIABLES_FIELD_NAME}")
        .build()
    )

    if (variables.isNotEmpty()) {
      addType(VariablesTypeSpecBuilder(variables, context).build())
    }

    return this
  }

  private fun TypeSpec.Builder.addQueryConstructor(hasVariables: Boolean): TypeSpec.Builder {
    val methodBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
    if (hasVariables) {
      methodBuilder.addParameter(ParameterSpec.builder(OPERATION_VARIABLES_CLASS_NAME, VARIABLES_FIELD_NAME).build())
    }
    methodBuilder.addQueryConstructorCode(hasVariables)
    return addMethod(methodBuilder.build())
  }

  private fun MethodSpec.Builder.addQueryConstructorCode(hasVariables: Boolean): MethodSpec.Builder {
    val codeBuilder = CodeBlock.builder()
    if (hasVariables) {
      codeBuilder.addStatement("this.\$L = \$L", VARIABLES_FIELD_NAME, VARIABLES_FIELD_NAME)
    } else {
      codeBuilder.addStatement("this.\$L = \$T.EMPTY_VARIABLES", VARIABLES_FIELD_NAME, ClassNames.GRAPHQL_OPERATION)
    }
    return addCode(codeBuilder.build())
  }

  private fun TypeSpec.Builder.addResponseFieldMapperMethod(): TypeSpec.Builder {
    return addMethod(MethodSpec.methodBuilder("responseFieldMapper")
        .addAnnotation(Annotations.OVERRIDE)
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(ClassName.get(ResponseFieldMapper::class.java), DATA_VAR_TYPE))
        .addStatement("return new \$L.\$L()", Operation.DATA_TYPE_NAME, Util.MAPPER_TYPE_NAME)
        .build())
  }

  private fun wrapperType(context: CodeGenerationContext) = when (context.nullableValueGenerationType) {
    NullableValueGenerationType.GUAVA_OPTIONAL -> ClassNames.parameterizedGuavaOptional(DATA_VAR_TYPE)
    NullableValueGenerationType.APOLLO_OPTIONAL -> ClassNames.parameterizedOptional(DATA_VAR_TYPE)
    else -> DATA_VAR_TYPE
  }

  companion object {
    private val OPERATION_DEFINITION_FIELD_NAME = "OPERATION_DEFINITION"
    private val QUERY_DOCUMENT_FIELD_NAME = "QUERY_DOCUMENT"
    private val QUERY_DOCUMENT_ACCESSOR_NAME = "queryDocument"
    private val VARIABLES_FIELD_NAME = "variables"
    private val VARIABLES_ACCESSOR_NAME = "variables"
  }
}
