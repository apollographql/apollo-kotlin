package com.apollostack.compiler

import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class ObjectBuilderTypeSpec(
    val targetObjectName: String,
    val targetObjectClassName: ClassName,
    val fields: List<Pair<String, TypeName>>
) {
  fun build(): TypeSpec {
    return TypeSpec
        .classBuilder(builderClassName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addBuilderTargetField()
        .addMethod(MethodSpec.constructorBuilder().build())
        .addBuilderMethods(builderClassName, fields)
        .addBuilderBuildMethod()
        .build()
  }

  private fun TypeSpec.Builder.addBuilderTargetField() =
      addField(FieldSpec
          .builder(targetObjectClassName, targetObjectName, Modifier.PRIVATE, Modifier.FINAL)
          .initializer("new \$T()", targetObjectClassName)
          .build())

  private fun TypeSpec.Builder.addBuilderMethods(builderClassName: ClassName,
      fields: List<Pair<String, TypeName>>): TypeSpec.Builder {
    fields.forEach { field ->
      val fieldName = field.first
      addMethod(MethodSpec
          .methodBuilder(fieldName)
          .addModifiers(Modifier.PUBLIC)
          .addParameter(ParameterSpec
              .builder(field.second, fieldName)
              .build())
          .returns(builderClassName)
          .addStatement("\$L.\$L = \$L", targetObjectName, fieldName, fieldName)
          .addStatement("return this")
          .build()
      )
    }
    return this
  }

  private fun TypeSpec.Builder.addBuilderBuildMethod() =
      addMethod(MethodSpec
          .methodBuilder("build")
          .addModifiers(Modifier.PUBLIC)
          .returns(targetObjectClassName)
          .addStatement("return \$L", targetObjectName)
          .build())

  companion object {
    private val builderClassName = ClassName.get("", "Builder")

    fun builderFactoryMethod(): MethodSpec =
        MethodSpec
            .methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(builderClassName)
            .addStatement("return new \$T()", builderClassName)
            .build()
  }
}