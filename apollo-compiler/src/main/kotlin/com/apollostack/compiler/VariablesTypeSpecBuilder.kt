package com.apollostack.compiler

import com.apollostack.compiler.ir.GraphQLType
import com.apollostack.compiler.ir.TypeDeclarationField
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
          .addVariableFields()
          .addConstructor()
          .addVariableAccessors()
          .addBuilder()
          .build()

  private fun Variable.javaTypeName() = GraphQLType.resolveByName(type, !type.endsWith("!")).toJavaTypeName()

  private fun TypeSpec.Builder.addVariableFields(): TypeSpec.Builder =
      addFields(variables.map { variable ->
        FieldSpec
            .builder(variable.javaTypeName(), variable.name.decapitalize())
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build()
      })

  private fun TypeSpec.Builder.addConstructor(): TypeSpec.Builder {
    val fieldInitializeCodeBuilder = variables.map {
      CodeBlock.of("this.\$L = \$L;\n", it.name.decapitalize(), it.name.decapitalize())
    }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

    return addMethod(MethodSpec
        .constructorBuilder()
        .addParameters(variables.map {
          ParameterSpec.builder(it.javaTypeName(), it.name.decapitalize()).build()
        })
        .addCode(fieldInitializeCodeBuilder.build())
        .build()
    )
  }

  private fun TypeSpec.Builder.addVariableAccessors(): TypeSpec.Builder =
      addMethods(variables.map { variable ->
        MethodSpec
            .methodBuilder(variable.name)
            .addModifiers(Modifier.PUBLIC)
            .returns(variable.javaTypeName())
            .addStatement("return \$L", variable.name.decapitalize())
            .build()
      })

  private fun TypeSpec.Builder.addBuilder(): TypeSpec.Builder {
    if (variables.isEmpty()) {
      return this
    } else {
      val builderFields = variables.map { it.name.decapitalize() to it.graphQLType() }
      return addMethod(BuilderTypeSpecBuilder.builderFactoryMethod())
          .addType(BuilderTypeSpecBuilder(VARIABLES_FIELD_NAME, VARIABLES_TYPE_NAME, builderFields, emptyMap()).build())
    }
  }

  companion object {
    private val VARIABLES_CLASS_NAME: String = "Variables"
    private val VARIABLES_FIELD_NAME: String = VARIABLES_CLASS_NAME.decapitalize()
    private val VARIABLES_TYPE_NAME: ClassName = ClassName.get("", VARIABLES_CLASS_NAME)
    private fun Variable.graphQLType() = GraphQLType.resolveByName(type, !type.endsWith("!"))
  }
}