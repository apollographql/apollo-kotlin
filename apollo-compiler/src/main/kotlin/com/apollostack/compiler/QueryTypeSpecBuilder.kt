package com.apollostack.compiler

import com.apollostack.api.GraphQLQuery
import com.apollostack.compiler.ir.Fragment
import com.apollostack.compiler.ir.Operation
import com.squareup.javapoet.*
import java.util.*
import javax.lang.model.element.Modifier

class QueryTypeSpecBuilder(
    val operation: Operation,
    val fragments: List<Fragment>
) {
  fun build(): TypeSpec {
    val queryClassName = operation.operationName.capitalize()
    val queryInterfaceTypeName = ParameterizedTypeName.get(ClassName.get(GraphQLQuery::class.java),
        ClassName.get("", "$queryClassName.${Operation.INTERFACE_TYPE_SPEC_NAME}"))
    return TypeSpec.classBuilder(queryClassName)
        .addSuperinterface(queryInterfaceTypeName)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addOperationSourceDefinition(operation)
        .addFragmentSourceDefinitions(fragments)
        .addType(operation.toTypeSpec())
        .build()
  }

  private fun TypeSpec.Builder.addOperationSourceDefinition(operation: Operation): TypeSpec.Builder {
    addField(FieldSpec.builder(ClassName.get(String::class.java), OPERATION_SOURCE_FIELD_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("\$S", operation.source)
        .build()
    )
    addMethod(MethodSpec.methodBuilder("getOperationDefinition")
        .addAnnotation(JavaPoetUtils.OVERRIDE_ANNOTATION)
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassName.get(String::class.java))
        .addStatement("return $OPERATION_SOURCE_FIELD_NAME")
        .build()
    )
    return this
  }

  private fun List<Fragment>.toSourceDefinitionCode(): CodeBlock {
    val codeBlockBuilder = CodeBlock.builder()
    codeBlockBuilder.add("\$T.unmodifiableList(", ClassName.get(Collections::class.java))
    codeBlockBuilder.add("\$T.asList(\n", ClassName.get(Arrays::class.java))
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
      addMethod(MethodSpec.methodBuilder("getFragmentDefinitions")
          .addAnnotation(JavaPoetUtils.OVERRIDE_ANNOTATION)
          .addModifiers(Modifier.PUBLIC)
          .returns(ParameterizedTypeName.get(ClassName.get(List::class.java), ClassName.get(String::class.java)))
          .addStatement("return \$T.emptyList()", ClassName.get(Collections::class.java))
          .build()
      )
    } else {
      addField(
          FieldSpec.builder(
              ParameterizedTypeName.get(ClassName.get(List::class.java), ClassName.get(String::class.java)),
              FRAGMENT_SOURCES_FIELD_NAME)
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
              .initializer(fragments.toSourceDefinitionCode())
              .build()
      )
      addMethod(MethodSpec.methodBuilder("getFragmentDefinitions")
          .addAnnotation(Override::class.java)
          .addModifiers(Modifier.PUBLIC)
          .returns(ParameterizedTypeName.get(ClassName.get(List::class.java), ClassName.get(String::class.java)))
          .addStatement("return $FRAGMENT_SOURCES_FIELD_NAME")
          .build()
      )
    }
    return this
  }

  companion object {
    private val OPERATION_SOURCE_FIELD_NAME = "OPERATION_DEFINITION"
    private val FRAGMENT_SOURCES_FIELD_NAME = "FRAGMENT_DEFINITIONS"
  }
}