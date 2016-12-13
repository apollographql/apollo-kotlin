package com.apollostack.compiler

import com.apollostack.compiler.ir.CodeGenerator
import com.apollostack.compiler.ir.Fragment
import com.apollostack.compiler.ir.Operation
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
          .returns(ParameterizedTypeName.get(
              ClassNames.LIST, ClassNames.STRING))
          .addStatement("return \$T.emptyList()", ClassNames.COLLECTIONS)
          .build()
      )
    } else {
      addField(
          FieldSpec.builder(
              ParameterizedTypeName.get(ClassNames.LIST, ClassNames.STRING),
              FRAGMENT_SOURCES_FIELD_NAME)
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
              .initializer(fragments.toSourceDefinitionCode())
              .build()
      )
      addMethod(MethodSpec.methodBuilder(FRAGMENT_DEFINITIONS_ACCESSOR_NAME)
          .addAnnotation(Annotations.OVERRIDE)
          .addModifiers(Modifier.PUBLIC)
          .returns(ParameterizedTypeName.get(
              ClassNames.LIST, ClassNames.STRING))
          .addStatement("return $FRAGMENT_SOURCES_FIELD_NAME")
          .build()
      )
    }
    return this
  }

  companion object {
    private val OPERATION_SOURCE_FIELD_NAME = "OPERATION_DEFINITION"
    private val OPERATION_DEFINITION_ACCESSOR_NAME = "operationDefinition"
    private val FRAGMENT_SOURCES_FIELD_NAME = "FRAGMENT_DEFINITIONS"
    private val FRAGMENT_DEFINITIONS_ACCESSOR_NAME = "fragmentDefinitions"
  }
}