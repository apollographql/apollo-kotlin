package com.apollostack.compiler

import com.apollostack.compiler.ir.CodeGenerator
import com.apollostack.compiler.ir.Fragment
import com.apollostack.compiler.ir.Operation
import com.apollostack.compiler.ir.Variable
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class QueryTypeSpecBuilder(
    val operation: Operation,
    val fragments: List<Fragment>
) : CodeGenerator {
  override fun toTypeSpec(): TypeSpec {
    val queryClassName = operation.operationName.capitalize()
    return TypeSpec.classBuilder(queryClassName)
        .addSuperinterface(ClassNames.QUERY)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addOperationSourceDefinition(operation)
        .addFragmentSourceDefinitions(fragments)
        .addVariablesDefinitions(operation.variables)
        .addType(operation.toTypeSpec())
        .build()
  }

  private fun TypeSpec.Builder.addOperationSourceDefinition(operation: Operation): TypeSpec.Builder {
    addField(FieldSpec.builder(ClassNames.STRING, OPERATION_SOURCE_FIELD_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("\$S", operation.source)
        .build()
    )
    addMethod(MethodSpec.methodBuilder(OPERATION_DEFINITION_ACCESSOR_NAME)
        .addAnnotation(Annotations.OVERRIDE)
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassNames.STRING)
        .addStatement("return $OPERATION_SOURCE_FIELD_NAME")
        .build()
    )
    return this
  }

  private fun List<Fragment>.toSourceDefinitionCode(): CodeBlock {
    val codeBlockBuilder = CodeBlock.builder()
        .add("\$T.unmodifiableList(", ClassNames.COLLECTIONS)
        .add("\$T.asList(\n", ClassNames.ARRAYS)
        .indent()
    forEachIndexed { i, fragment ->
      codeBlockBuilder.add("\$S\$L", fragment.source, if (i < this.size - 1) "," else "")
    }
    return codeBlockBuilder.unindent()
        .add("\n)")
        .add(")")
        .build()
  }

  private fun TypeSpec.Builder.addFragmentSourceDefinitions(fragments: List<Fragment>): TypeSpec.Builder {
    if (fragments.isEmpty()) {
      addMethod(MethodSpec.methodBuilder(FRAGMENT_DEFINITIONS_ACCESSOR_NAME)
          .addAnnotation(Annotations.OVERRIDE)
          .addModifiers(Modifier.PUBLIC)
          .returns(ClassNames.parameterizedListOf(ClassNames.STRING))
          .addStatement("return \$T.emptyList()", ClassNames.COLLECTIONS)
          .build()
      )
    } else {
      addField(
          FieldSpec.builder(ClassNames.parameterizedListOf(ClassNames.STRING),
              FRAGMENT_SOURCES_FIELD_NAME)
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
              .initializer(fragments.toSourceDefinitionCode())
              .build()
      )
      addMethod(MethodSpec.methodBuilder(FRAGMENT_DEFINITIONS_ACCESSOR_NAME)
          .addAnnotation(Annotations.OVERRIDE)
          .addModifiers(Modifier.PUBLIC)
          .returns(ClassNames.parameterizedListOf(ClassNames.STRING))
          .addStatement("return $FRAGMENT_SOURCES_FIELD_NAME")
          .build()
      )
    }
    return this
  }

  private fun TypeSpec.Builder.addVariablesDefinitions(variables: List<Variable>): TypeSpec.Builder {
    if (variables.isEmpty()) {
      addMethod(MethodSpec.methodBuilder(VARIABLE_DEFINITIONS_ACCESSOR_NAME)
          .addAnnotation(Annotations.OVERRIDE)
          .addModifiers(Modifier.PUBLIC)
          .returns(ClassNames.parameterizedMapOf(ClassNames.STRING, ClassNames.OBJECT))
          .addStatement("return \$T.emptyMap()", ClassNames.COLLECTIONS)
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
              .addAnnotation(Annotations.NONNULL).build())
          .addModifiers(Modifier.PUBLIC)
          .addStatement("this.\$L = \$L", VARIABLES_FIELD_NAME, VARIABLES_FIELD_NAME)
          .build()
      )
      addMethod(MethodSpec.methodBuilder(VARIABLE_DEFINITIONS_ACCESSOR_NAME)
          .addAnnotation(Annotations.OVERRIDE)
          .addModifiers(Modifier.PUBLIC)
          .returns(ClassNames.parameterizedMapOf(ClassNames.STRING, ClassNames.OBJECT))
          .addStatement("return \$L.\$L", VARIABLES_FIELD_NAME, QueryVariablesTypeSpecBuilder.VARIABLES_MAP_FIELD_NAME)
          .build()
      )
      addMethod(MethodSpec.methodBuilder(VARIABLES_ACCESSOR_NAME)
          .addModifiers(Modifier.PUBLIC)
          .returns(QueryVariablesTypeSpecBuilder.VARIABLES_TYPE_NAME)
          .addStatement("return $VARIABLES_FIELD_NAME")
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
    private val VARIABLES_ACCESSOR_NAME = "variables"
  }
}