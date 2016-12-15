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
  private val QUERY_TYPE_NAME = operation.operationName.capitalize()
  private val QUERY_VARIABLES_CLASS_NAME = ClassName.get("", "$QUERY_TYPE_NAME.Variables")

  override fun toTypeSpec(): TypeSpec {
    return TypeSpec.classBuilder(QUERY_TYPE_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addQuerySuperInterface(operation.variables.isNotEmpty())
        .addOperationSourceDefinition(operation)
        .addFragmentSourceDefinitions(fragments)
        .addQueryFields(operation.variables.isNotEmpty())
        .addQueryConstructor(operation.variables.isNotEmpty())
        .addVariablesTypeDefinition(operation.variables)
        .addType(operation.toTypeSpec())
        .build()
  }

  private fun TypeSpec.Builder.addQuerySuperInterface(hasVariables: Boolean): TypeSpec.Builder {
    return if (hasVariables) {
      addSuperinterface(ParameterizedTypeName.get(ClassNames.API_QUERY, QUERY_VARIABLES_CLASS_NAME))
    } else {
      addSuperinterface(ParameterizedTypeName.get(ClassNames.API_QUERY, ClassNames.API_QUERY_VARIABLES))
    }
  }

  private fun TypeSpec.Builder.addOperationSourceDefinition(operation: Operation): TypeSpec.Builder {
    return addField(FieldSpec.builder(ClassNames.STRING, OPERATION_SOURCE_FIELD_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("\$S", operation.source)
        .build()
    )
  }

  private fun TypeSpec.Builder.addFragmentSourceDefinitions(fragments: List<Fragment>): TypeSpec.Builder {
    return addField(FieldSpec.builder(ClassNames.parameterizedListOf(ClassNames.STRING), FRAGMENT_SOURCES_FIELD_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer(fragments.toSourceDefinitionCode())
        .build()
    )
  }

  private fun List<Fragment>.toSourceDefinitionCode(): CodeBlock {
    val codeBuilder = CodeBlock.builder().add("\$T.unmodifiableList(", ClassNames.COLLECTIONS)
    if (isEmpty()) {
      codeBuilder.add("\$T.<\$T>emptyList()", ClassNames.COLLECTIONS, ClassNames.STRING)
    } else {
      codeBuilder.add("\$T.asList(\n", ClassNames.ARRAYS).indent()
      forEachIndexed { i, fragment ->
        codeBuilder.add("\$S\$L", fragment.source, if (i < this.size - 1) "," else "")
      }
      codeBuilder.unindent().add("\n)")
    }
    return codeBuilder.add(")").build()
  }

  private fun TypeSpec.Builder.addQueryFields(hasVariables: Boolean): TypeSpec.Builder {
    addField(FieldSpec.builder(ClassNames.STRING, QUERY_FIELD_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build()
    )

    addMethod(MethodSpec.methodBuilder(QUERY_ACCESSOR_NAME)
        .addAnnotation(Annotations.OVERRIDE)
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassNames.STRING)
        .addStatement("return $QUERY_FIELD_NAME")
        .build()
    )

    val queryFieldClassName = if (hasVariables) QUERY_VARIABLES_CLASS_NAME else ClassNames.API_QUERY_VARIABLES
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

    return this
  }

  private fun TypeSpec.Builder.addQueryConstructor(hasVariables: Boolean): TypeSpec.Builder {
    val methodBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
    if (hasVariables) {
      methodBuilder.addParameter(ParameterSpec.builder(QUERY_VARIABLES_CLASS_NAME, VARIABLES_FIELD_NAME).build())
    }
    methodBuilder.addQueryConstructorCode(hasVariables)
    return addMethod(methodBuilder.build())
  }

  private fun MethodSpec.Builder.addQueryConstructorCode(hasVariables: Boolean): MethodSpec.Builder {
    val codeBuilder = CodeBlock.builder()
    if (hasVariables) {
      codeBuilder.addStatement("this.\$L = \$L", VARIABLES_FIELD_NAME, VARIABLES_FIELD_NAME)
    } else {
      codeBuilder.addStatement("this.\$L = \$T.EMPTY_VARIABLES", VARIABLES_FIELD_NAME, ClassNames.API_QUERY)
    }
    codeBuilder.addStatement("\$T stringBuilder = new \$T(\$L)", ClassNames.STRING_BUILDER, ClassNames.STRING_BUILDER,
        OPERATION_SOURCE_FIELD_NAME)
        .addStatement("stringBuilder.append(\$S)", "\n")
        .beginControlFlow("for (\$T fragmentDefinition : $FRAGMENT_SOURCES_FIELD_NAME)", ClassNames.STRING)
        .addStatement("stringBuilder.append(\$S)", "\n")
        .addStatement("stringBuilder.append(fragmentDefinition)")
        .endControlFlow()
        .addStatement("$QUERY_FIELD_NAME = stringBuilder.toString()")
    return addCode(codeBuilder.build())
  }

  private fun TypeSpec.Builder.addVariablesTypeDefinition(variables: List<Variable>): TypeSpec.Builder {
    if (variables.isNotEmpty()) {
      addType(QueryVariablesTypeSpecBuilder(variables).build())
    }
    return this
  }

  companion object {
    private val OPERATION_SOURCE_FIELD_NAME = "OPERATION_DEFINITION"
    private val FRAGMENT_SOURCES_FIELD_NAME = "FRAGMENT_DEFINITIONS"
    private val QUERY_FIELD_NAME = "query"
    private val QUERY_ACCESSOR_NAME = "operationDefinition"
    private val VARIABLES_FIELD_NAME = "variables"
    private val VARIABLES_ACCESSOR_NAME = "variables"
  }
}