package com.apollographql.android.compiler

import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.android.compiler.ir.*
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class OperationTypeSpecBuilder(
    val operation: Operation,
    val fragments: List<Fragment>
) : CodeGenerator {
  private val OPERATION_TYPE_NAME = operation.operationName.capitalize()
  private val DATA_VAR_TYPE = ClassName.get("", "$OPERATION_TYPE_NAME.Data")

  override fun toTypeSpec(context: CodeGenerationContext): TypeSpec {
    val newContext = context.copy(reservedTypeNames = context.reservedTypeNames.plus(OPERATION_TYPE_NAME))
    return TypeSpec.classBuilder(OPERATION_TYPE_NAME)
        .addAnnotation(Annotations.GENERATED_BY_APOLLO)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addQuerySuperInterface(context)
        .addOperationDefinition(operation)
        .addQueryDocumentDefinition(fragments, newContext)
        .addConstructor(context)
        .addMethod(wrapDataMethod(context))
        .addVariablesDefinition(operation.variables, newContext)
        .addResponseFieldMapperMethod()
        .addBuilder(context)
        .addType(operation.toTypeSpec(newContext))
        .build()
        .flatten(excludeTypeNames = listOf(Util.MAPPER_TYPE_NAME, SchemaTypeSpecBuilder.FRAGMENTS_TYPE_NAME))
  }

  private fun TypeSpec.Builder.addQuerySuperInterface(context: CodeGenerationContext): TypeSpec.Builder {
    val isMutation = operation.operationType == "mutation"
    val superInterfaceClassName = if (isMutation) ClassNames.GRAPHQL_MUTATION else ClassNames.GRAPHQL_QUERY
    return if (operation.variables.isNotEmpty()) {
      addSuperinterface(ParameterizedTypeName.get(superInterfaceClassName, DATA_VAR_TYPE,
          wrapperType(context), variablesType()))
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
      val className = ClassName.get(context.fragmentsPackage, it.formatClassName())
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
            if (context.nullableValueType != NullableValueType.ANNOTATED) {
              "return Optional.fromNullable(data)"
            } else {
              "return data"
            })
        .build()
  }

  private fun TypeSpec.Builder.addVariablesDefinition(variables: List<Variable>, context: CodeGenerationContext):
      TypeSpec.Builder {
    addField(FieldSpec.builder(variablesType(), VARIABLES_VAR)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build()
    )

    addMethod(MethodSpec.methodBuilder(VARIABLES_VAR)
        .addAnnotation(Annotations.OVERRIDE)
        .addModifiers(Modifier.PUBLIC)
        .returns(variablesType())
        .addStatement("return \$L", VARIABLES_VAR)
        .build()
    )

    if (variables.isNotEmpty()) {
      addType(VariablesTypeSpecBuilder(variables, context).build())
    }

    return this
  }

  private fun TypeSpec.Builder.addResponseFieldMapperMethod(): TypeSpec.Builder {
    return addMethod(MethodSpec.methodBuilder("responseFieldMapper")
        .addAnnotation(Annotations.OVERRIDE)
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(ClassName.get(ResponseFieldMapper::class.java), DATA_VAR_TYPE))
        .addStatement("return new \$L.\$L()", Operation.DATA_TYPE_NAME, Util.MAPPER_TYPE_NAME)
        .build())
  }

  private fun wrapperType(context: CodeGenerationContext) = when (context.nullableValueType) {
    NullableValueType.GUAVA_OPTIONAL -> ClassNames.parameterizedGuavaOptional(DATA_VAR_TYPE)
    NullableValueType.APOLLO_OPTIONAL -> ClassNames.parameterizedOptional(DATA_VAR_TYPE)
    else -> DATA_VAR_TYPE
  }

  private fun TypeSpec.Builder.addConstructor(context: CodeGenerationContext): TypeSpec.Builder {
    val code = if (operation.variables.isEmpty()) {
      CodeBlock.of("this.\$L = \$T.EMPTY_VARIABLES;\n", VARIABLES_VAR, ClassNames.GRAPHQL_OPERATION)
    } else {
      val builder = operation.variables.map {
        val name = it.name.decapitalize()
        val type = JavaTypeResolver(context, context.typesPackage).resolve(it.type)
        val optional = !it.type.endsWith("!")
        if (!type.isPrimitive && !optional) {
          CodeBlock.of("\$T.checkNotNull(\$L, \$S);\n", ClassNames.API_UTILS, name, "$name == null")
        } else {
          CodeBlock.of("")
        }
      }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

      operation.variables
          .map { it.name.decapitalize() }
          .mapIndexed { i, v -> CodeBlock.of("\$L\$L", if (i > 0) ", " else "", v) }
          .fold(builder.add("\$L = new \$T(", VARIABLES_VAR, variablesType()), CodeBlock.Builder::add)
          .add(");\n")
          .build()
    }
    return MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameters(operation.variables
            .map { it.name.decapitalize() to it.type }
            .map { it.first to JavaTypeResolver(context, context.typesPackage).resolve(it.second) }
            .map { ParameterSpec.builder(it.second.unwrapOptionalType(), it.first).build() })
        .addCode(code)
        .build()
        .let { addMethod(it) }
  }

  private fun TypeSpec.Builder.addBuilder(context: CodeGenerationContext): TypeSpec.Builder {
    addMethod(BuilderTypeSpecBuilder.builderFactoryMethod())
    if (operation.variables.isEmpty()) {
      return BuilderTypeSpecBuilder(ClassName.get("", OPERATION_TYPE_NAME), emptyList(), emptyMap())
          .let { addType(it.build()) }
    }
    return operation.variables
        .map { it.name.decapitalize() to it.type }
        .map { it.first to JavaTypeResolver(context, context.typesPackage).resolve(it.second).unwrapOptionalType() }
        .let { BuilderTypeSpecBuilder(ClassName.get("", OPERATION_TYPE_NAME), it, emptyMap()) }
        .let { addType(it.build()) }
  }

  private fun variablesType() =
      if (operation.variables.isNotEmpty())
        ClassName.get("", "$OPERATION_TYPE_NAME.Variables")
      else
        ClassNames.GRAPHQL_OPERATION_VARIABLES

  companion object {
    private val OPERATION_DEFINITION_FIELD_NAME = "OPERATION_DEFINITION"
    private val QUERY_DOCUMENT_FIELD_NAME = "QUERY_DOCUMENT"
    private val QUERY_DOCUMENT_ACCESSOR_NAME = "queryDocument"
    private val VARIABLES_VAR = "variables"
  }
}
