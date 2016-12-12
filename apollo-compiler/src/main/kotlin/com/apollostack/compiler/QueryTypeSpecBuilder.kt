package com.apollostack.compiler

import com.apollostack.compiler.ir.Fragment
import com.apollostack.compiler.ir.Operation
import com.apollostack.compiler.ir.Variable
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class QueryTypeSpecBuilder(
    val operation: Operation,
    val fragments: List<Fragment>
) {
  fun build(): TypeSpec {
    val queryClassName = operation.operationName.capitalize()
    return TypeSpec.classBuilder(queryClassName)
        .addSuperinterface(JavaPoetUtils.GRAPH_QL_QUERY_CLASS_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addOperationSourceDefinition(operation)
        .addFragmentSourceDefinitions(fragments)
        .addVariablesDefinitions(operation.variables)
        .addType(operation.toTypeSpec())
        .build()
  }

  private fun TypeSpec.Builder.addOperationSourceDefinition(operation: Operation): TypeSpec.Builder {
    addField(FieldSpec.builder(JavaPoetUtils.STRING_CLASS_NAME, OPERATION_SOURCE_FIELD_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("\$S", operation.source)
        .build()
    )
    addMethod(MethodSpec.methodBuilder(OPERATION_DEFINITION_ACCESSOR_NAME)
        .addAnnotation(JavaPoetUtils.OVERRIDE_ANNOTATION)
        .addModifiers(Modifier.PUBLIC)
        .returns(JavaPoetUtils.STRING_CLASS_NAME)
        .addStatement("return $OPERATION_SOURCE_FIELD_NAME")
        .build()
    )
    return this
  }

  private fun List<Fragment>.toSourceDefinitionCode(): CodeBlock {
    val codeBlockBuilder = CodeBlock.builder()
    codeBlockBuilder.add("\$T.unmodifiableList(", JavaPoetUtils.COLLECTIONS_CLASS_NAME)
    codeBlockBuilder.add("\$T.asList(\n", JavaPoetUtils.ARRAYS_CLASS_NAME)
    codeBlockBuilder.indent()
    forEachIndexed { i, fragment ->
      codeBlockBuilder.add("\$S\$L", fragment.source, if (i < this.size - 1) "," else "")
    }
    codeBlockBuilder.unindent()
    codeBlockBuilder.add("\n)")
    codeBlockBuilder.add(")")
    return codeBlockBuilder.build()
  }

  private fun TypeSpec.Builder.addFragmentSourceDefinitions(fragments: List<Fragment>): TypeSpec.Builder {
    if (fragments.isEmpty()) {
      addMethod(MethodSpec.methodBuilder(FRAGMENT_DEFINITIONS_ACCESSOR_NAME)
          .addAnnotation(JavaPoetUtils.OVERRIDE_ANNOTATION)
          .addModifiers(Modifier.PUBLIC)
          .returns(JavaPoetUtils.parameterizedList(JavaPoetUtils.STRING_CLASS_NAME))
          .returns(ParameterizedTypeName.get(JavaPoetUtils.LIST_CLASS_NAME, JavaPoetUtils.STRING_CLASS_NAME))
          .addStatement("return \$T.emptyList()", JavaPoetUtils.COLLECTIONS_CLASS_NAME)
          .build()
      )
    } else {
      addField(
          FieldSpec.builder(JavaPoetUtils.parameterizedList(JavaPoetUtils.STRING_CLASS_NAME),
              FRAGMENT_SOURCES_FIELD_NAME)
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
              .initializer(fragments.toSourceDefinitionCode())
              .build()
      )
      addMethod(MethodSpec.methodBuilder(FRAGMENT_DEFINITIONS_ACCESSOR_NAME)
          .addAnnotation(JavaPoetUtils.OVERRIDE_ANNOTATION)
          .addModifiers(Modifier.PUBLIC)
          .returns(JavaPoetUtils.parameterizedList(JavaPoetUtils.STRING_CLASS_NAME))
          .returns(ParameterizedTypeName.get(JavaPoetUtils.LIST_CLASS_NAME, JavaPoetUtils.STRING_CLASS_NAME))
          .addStatement("return $FRAGMENT_SOURCES_FIELD_NAME")
          .build()
      )
    }
    return this
  }

  private fun TypeSpec.Builder.addVariablesDefinitions(variables: List<Variable>): TypeSpec.Builder {
    if (variables.isEmpty()) {
      addMethod(MethodSpec.methodBuilder(VARIABLE_DEFINITIONS_ACCESSOR_NAME)
          .addAnnotation(JavaPoetUtils.OVERRIDE_ANNOTATION)
          .addModifiers(Modifier.PUBLIC)
          .returns(JavaPoetUtils.parameterizedMap(JavaPoetUtils.STRING_CLASS_NAME, JavaPoetUtils.OBJECT_CLASS_NAME))
          .addStatement("return \$T.EMPTY_MAP", JavaPoetUtils.COLLECTIONS_CLASS_NAME)
          .build()
      )
    } else {
      addField(FieldSpec
          .builder(QueryVariablesTypeSpecBuilder.VARIABLES_TYPE_NAME, VARIABLES_FIELD_NAME)
          .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
          .build()
      )
      addMethod(MethodSpec.constructorBuilder()
          .addParameter(ParameterSpec.builder(QueryVariablesTypeSpecBuilder.VARIABLES_TYPE_NAME, VARIABLES_FIELD_NAME)
              .addAnnotation(JavaPoetUtils.NONNULL_ANNOTATION).build())
          .addModifiers(Modifier.PUBLIC)
          .addStatement("this.\$L = \$L", VARIABLES_FIELD_NAME, VARIABLES_FIELD_NAME)
          .build()
      )
      addMethod(MethodSpec.methodBuilder(VARIABLE_DEFINITIONS_ACCESSOR_NAME)
          .addAnnotation(JavaPoetUtils.OVERRIDE_ANNOTATION)
          .addModifiers(Modifier.PUBLIC)
          .returns(JavaPoetUtils.parameterizedMap(JavaPoetUtils.STRING_CLASS_NAME, JavaPoetUtils.OBJECT_CLASS_NAME))
          .addStatement("return \$L.\$L", VARIABLES_FIELD_NAME, QueryVariablesTypeSpecBuilder.VARIABLES_MAP_FIELD_NAME)
          .build()
      )
      addType(QueryVariablesTypeSpecBuilder(variables).build())
    }
    return this
  }

  companion object {
    private val OPERATION_SOURCE_FIELD_NAME = "OPERATION_DEFINITION"
    private val OPERATION_DEFINITION_ACCESSOR_NAME = "operationDefinition"
    private val FRAGMENT_SOURCES_FIELD_NAME = "FRAGMENT_DEFINITIONS"
    private val FRAGMENT_DEFINITIONS_ACCESSOR_NAME = "fragmentDefinitions"
    private val VARIABLE_DEFINITIONS_ACCESSOR_NAME = "variableDefinitions"
    private val VARIABLES_FIELD_NAME = "variables"
  }
}