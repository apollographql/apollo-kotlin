package com.apollographql.android.compiler

import com.apollographql.android.compiler.ir.Variable
import com.squareup.javapoet.*
import java.util.*
import javax.lang.model.element.Modifier

class VariablesTypeSpecBuilder(
    val variables: List<Variable>,
    val typesPackage: String,
    val customScalarTypeMap: Map<String, String>
) {
  fun build(): TypeSpec =
      TypeSpec.classBuilder(VARIABLES_CLASS_NAME)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .superclass(ClassNames.GRAPHQL_OPERATION_VARIABLES)
          .addVariableFields()
          .addValueMapField()
          .addConstructor()
          .addVariableAccessors()
          .addValueMapAccessor()
          .addBuilder()
          .build()

  private fun TypeSpec.Builder.addVariableFields(): TypeSpec.Builder =
      addFields(variables.map { variable ->
        FieldSpec
            .builder(variable.javaTypeName(customScalarTypeMap, typesPackage), variable.name.decapitalize())
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build()
      })

  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  private fun TypeSpec.Builder.addValueMapField(): TypeSpec.Builder =
      addField(FieldSpec.builder(ClassNames.parameterizedMapOf(java.lang.String::class.java, Object::class.java),
          VALUE_MAP_FIELD_NAME)
          .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
          .initializer("new \$T<>()", LinkedHashMap::class.java)
          .build())

  private fun TypeSpec.Builder.addConstructor(): TypeSpec.Builder {
    val fieldInitializeCode = variables.map {
      CodeBlock.of("this.\$L = \$L;\n", it.name.decapitalize(), it.name.decapitalize())
    }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

    val fieldMapInitializeCode = variables.map {
      CodeBlock.of("this.valueMap.put(\$S, \$L);\n", it.name, it.name.decapitalize())
    }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

    return addMethod(MethodSpec
        .constructorBuilder()
        .addParameters(variables.map {
          ParameterSpec.builder(it.javaTypeName(customScalarTypeMap, typesPackage), it.name.decapitalize()).build()
        })
        .addCode(fieldInitializeCode.build())
        .addCode(fieldMapInitializeCode.build())
        .build()
    )
  }

  private fun TypeSpec.Builder.addVariableAccessors(): TypeSpec.Builder =
      addMethods(variables.map { variable ->
        MethodSpec
            .methodBuilder(variable.name)
            .addModifiers(Modifier.PUBLIC)
            .returns(variable.javaTypeName(customScalarTypeMap, typesPackage))
            .addStatement("return \$L", variable.name.decapitalize())
            .build()
      })

  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  private fun TypeSpec.Builder.addValueMapAccessor(): TypeSpec.Builder =
      addMethod(MethodSpec.methodBuilder(VALUE_MAP_FIELD_NAME)
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(Override::class.java)
          .returns(ClassNames.parameterizedMapOf(java.lang.String::class.java, Object::class.java))
          .addStatement("return \$T.unmodifiableMap(\$L)", Collections::class.java, VALUE_MAP_FIELD_NAME)
          .build())

  private fun TypeSpec.Builder.addBuilder(): TypeSpec.Builder {
    if (variables.isEmpty()) {
      return this
    } else {
      val builderFields = variables.map { it.name.decapitalize() to it.javaTypeName(customScalarTypeMap, typesPackage) }
      return addMethod(BuilderTypeSpecBuilder.builderFactoryMethod())
          .addType(BuilderTypeSpecBuilder(VARIABLES_TYPE_NAME, builderFields, emptyMap()).build())
    }
  }

  companion object {
    private val VARIABLES_CLASS_NAME: String = "Variables"
    private val VARIABLES_TYPE_NAME: ClassName = ClassName.get("", VARIABLES_CLASS_NAME)
    private fun Variable.javaTypeName(customScalarTypeMap: Map<String, String>, packageName: String) =
        JavaTypeResolver(customScalarTypeMap, packageName).resolve(type, !type.endsWith("!"))

    private val VALUE_MAP_FIELD_NAME = "valueMap"
  }
}
