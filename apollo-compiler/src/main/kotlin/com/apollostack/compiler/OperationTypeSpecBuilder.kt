package com.apollostack.compiler

import com.apollostack.compiler.ir.*
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class OperationTypeSpecBuilder(
    val operation: Operation,
    val fragments: List<Fragment>
) : CodeGenerator {
  private val OPERATION_TYPE_NAME = operation.operationName.capitalize()
  private val OPERATION_VARIABLES_CLASS_NAME = ClassName.get("", "$OPERATION_TYPE_NAME.Variables")

  override fun toTypeSpec(abstractClass: Boolean, reservedTypeNames: List<String>,
      typeDeclarations: List<TypeDeclaration>, fragmentsPkgName: String, typesPkgName: String): TypeSpec {
    return TypeSpec.classBuilder(OPERATION_TYPE_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addQuerySuperInterface(operation.variables.isNotEmpty())
        .addOperationDefinition(operation)
        .addQueryDocumentDefinition(fragments)
        .addQueryConstructor(operation.variables.isNotEmpty())
        .addVariablesDefinition(operation.variables, typesPkgName)
        .addType(operation.toTypeSpec(abstractClass, reservedTypeNames, typeDeclarations, fragmentsPkgName, typesPkgName))
        .build()
  }

  private fun TypeSpec.Builder.addQuerySuperInterface(hasVariables: Boolean): TypeSpec.Builder {
    val isMutation = operation.operationType == "mutation"
    val superInterfaceClassName = if (isMutation) ClassNames.GRAPHQL_MUTATION else ClassNames.GRAPHQL_QUERY
    return if (hasVariables) {
      addSuperinterface(ParameterizedTypeName.get(superInterfaceClassName, OPERATION_VARIABLES_CLASS_NAME))
    } else {
      addSuperinterface(ParameterizedTypeName.get(superInterfaceClassName, ClassNames.GRAPHQL_OPERATION_VARIABLES))
    }
  }

  private fun TypeSpec.Builder.addOperationDefinition(operation: Operation): TypeSpec.Builder {
    return addField(FieldSpec.builder(ClassNames.STRING, OPERATION_DEFINITION_FIELD_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("\$S", operation.source)
        .build()
    )
  }

  private fun TypeSpec.Builder.addQueryDocumentDefinition(fragments: List<Fragment>): TypeSpec.Builder {
    val initializeCodeBuilder = CodeBlock.builder().add(OPERATION_DEFINITION_FIELD_NAME)
    fragments.forEach {
      initializeCodeBuilder
          .add(" + \$S\n", "\n")
          .add(" + \$L.\$L", it.interfaceTypeName(), Fragment.FRAGMENT_DEFINITION_FIELD_NAME)
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

  private fun TypeSpec.Builder.addVariablesDefinition(variables: List<Variable>, typesPkgName: String): TypeSpec.Builder {
    val queryFieldClassName = if (variables.isNotEmpty()) OPERATION_VARIABLES_CLASS_NAME else ClassNames.GRAPHQL_OPERATION_VARIABLES
    addField(FieldSpec.builder(queryFieldClassName, VARIABLES_FIELD_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build()
    )

    addMethod(MethodSpec.methodBuilder(VARIABLES_ACCESSOR_NAME)
        .addAnnotation(Annotations.OVERRIDE)
        .addModifiers(Modifier.PUBLIC)
        .returns(queryFieldClassName)
        .addStatement("return $VARIABLES_FIELD_NAME")
        .build()
    )

    if (variables.isNotEmpty()) {
      addType(VariablesTypeSpecBuilder(variables, typesPkgName).build())
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

  companion object {
    private val OPERATION_DEFINITION_FIELD_NAME = "OPERATION_DEFINITION"
    private val QUERY_DOCUMENT_FIELD_NAME = "QUERY_DOCUMENT"
    private val QUERY_DOCUMENT_ACCESSOR_NAME = "queryDocument"
    private val VARIABLES_FIELD_NAME = "variables"
    private val VARIABLES_ACCESSOR_NAME = "variables"
  }
}
