package com.apollostack.compiler

import com.apollostack.compiler.ir.GraphQLType
import com.apollostack.compiler.ir.Variable
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class VariablesTypeSpecBuilder(
    val variables: List<Variable>
) {
  fun build(): TypeSpec =
      TypeSpec.classBuilder(VARIABLES_CLASS_NAME)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .superclass(ClassNames.GRAPHQL_OPERATION_VARIABLES)
          .addMethod(MethodSpec.constructorBuilder().build())
          .addVariableFields(variables)
          .addVariableAccessors(variables)
          .addBuilder(variables)
          .build()

  private fun Variable.javaTypeName() = GraphQLType.resolveByName(type, !type.endsWith("!")).toJavaTypeName()

  private fun TypeSpec.Builder.addVariableFields(variables: List<Variable>): TypeSpec.Builder {
    variables.forEach { variable ->
      addField(FieldSpec
          .builder(variable.javaTypeName(), variable.name.decapitalize())
          .build())
    }
    return this
  }

  private fun TypeSpec.Builder.addVariableAccessors(variables: List<Variable>): TypeSpec.Builder {
    variables.forEach { variable ->
      addMethod(MethodSpec
          .methodBuilder(variable.name)
          .addModifiers(Modifier.PUBLIC)
          .returns(variable.javaTypeName())
          .addStatement("return \$L", variable.name.decapitalize())
          .build())
    }
    return this
  }

  private fun TypeSpec.Builder.addBuilder(variables: List<Variable>): TypeSpec.Builder {
    if (variables.isEmpty()) {
      return this
    } else {
      val builderFields = variables.map { it.name.decapitalize() to it.javaTypeName() }
      return addMethod(BuilderTypeSpecBuilder.builderFactoryMethod())
          .addType(BuilderTypeSpecBuilder(VARIABLES_FIELD_NAME, VARIABLES_TYPE_NAME, builderFields).build())
    }
  }

  companion object {
    private val VARIABLES_CLASS_NAME: String = "Variables"
    private val VARIABLES_FIELD_NAME: String = VARIABLES_CLASS_NAME.decapitalize()
    private val VARIABLES_TYPE_NAME: ClassName = ClassName.get("", VARIABLES_CLASS_NAME)
  }
}