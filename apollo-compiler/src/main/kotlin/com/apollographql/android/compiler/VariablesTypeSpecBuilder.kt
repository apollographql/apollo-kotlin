package com.apollographql.android.compiler

import com.apollographql.android.compiler.ir.Variable
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class VariablesTypeSpecBuilder(
    val variables: List<Variable>,
    val typesPackage: String
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

  private fun TypeSpec.Builder.addVariableFields(): TypeSpec.Builder =
      addFields(variables.map { variable ->
        FieldSpec
            .builder(variable.javaTypeName(typesPackage), variable.name.decapitalize())
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
          ParameterSpec.builder(it.javaTypeName(typesPackage), it.name.decapitalize()).build()
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
            .returns(variable.javaTypeName(typesPackage))
            .addStatement("return \$L", variable.name.decapitalize())
            .build()
      })

  private fun TypeSpec.Builder.addBuilder(): TypeSpec.Builder {
    if (variables.isEmpty()) {
      return this
    } else {
      val builderFields = variables.map { it.name.decapitalize() to it.javaTypeName(typesPackage) }
      return addMethod(BuilderTypeSpecBuilder.builderFactoryMethod())
          .addType(BuilderTypeSpecBuilder(VARIABLES_TYPE_NAME, builderFields, emptyMap(), typesPackage).build())
    }
  }

  companion object {
    private val VARIABLES_CLASS_NAME: String = "Variables"
    private val VARIABLES_TYPE_NAME: ClassName = ClassName.get("", VARIABLES_CLASS_NAME)
    private fun Variable.javaTypeName(packageName: String) =
        JavaTypeResolver(packageName).resolve(type, !type.endsWith("!"))
  }
}
