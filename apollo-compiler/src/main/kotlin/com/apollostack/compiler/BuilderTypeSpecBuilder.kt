package com.apollostack.compiler

import com.apollostack.compiler.ir.graphql.Type
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class BuilderTypeSpecBuilder(
    val targetObjectClassName: ClassName,
    val fields: List<Pair<String, Type>>,
    val fieldDefaultValues: Map<String, Any?>,
    val typesPkgName: String
) {
  fun build(): TypeSpec {
    return TypeSpec.classBuilder(builderClassName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addBuilderFields()
        .addMethod(MethodSpec.constructorBuilder().build())
        .addBuilderMethods()
        .addBuilderBuildMethod()
        .build()
  }

  private fun TypeSpec.Builder.addBuilderFields(): TypeSpec.Builder =
      addFields(fields.map {
        val fieldName = it.first
        val fieldType = it.second
        val defaultValue = fieldDefaultValues[fieldName]?.let { (it as? Number)?.castTo(fieldType) ?: it }
        FieldSpec.builder(fieldType.toJavaTypeName(typesPkgName), fieldName)
            .addModifiers(Modifier.PRIVATE)
            .initializer(defaultValue?.let { CodeBlock.of("\$L", it) } ?: CodeBlock.of(""))
            .build()
      })

  private fun TypeSpec.Builder.addBuilderMethods(): TypeSpec.Builder =
      addMethods(fields.map {
        val fieldName = it.first
        val fieldType = it.second
        MethodSpec.methodBuilder(fieldName)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ParameterSpec.builder(fieldType.toJavaTypeName(), fieldName).build())
            .returns(builderClassName)
            .addStatement("this.\$L = \$L", fieldName, fieldName)
            .addStatement("return this")
            .build()
      })

  private fun TypeSpec.Builder.addBuilderBuildMethod(): TypeSpec.Builder {
    val validationCodeBuilder = fields.filter {
      val fieldType = it.second.toJavaTypeName()
      !fieldType.isPrimitive && fieldType.annotations.contains(Annotations.NONNULL)
    }.map {
      val fieldName = it.first
      CodeBlock.of("if (\$L == null) throw new \$T(\$S);\n", fieldName,
          ClassNames.ILLEGAL_STATE_EXCEPTION, "$fieldName can't be null")
    }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

    return addMethod(MethodSpec
        .methodBuilder("build")
        .addModifiers(Modifier.PUBLIC)
        .returns(targetObjectClassName)
        .addCode(validationCodeBuilder.build())
        .addStatement("return new \$T\$L", targetObjectClassName,
            fields.map { it.first }.joinToString(prefix = "(", separator = ", ", postfix = ")"))
        .build())
  }

  companion object {
    private val builderClassName = ClassName.get("", "Builder")

    fun builderFactoryMethod(): MethodSpec =
        MethodSpec
            .methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(builderClassName)
            .addStatement("return new \$T()", builderClassName)
            .build()

    private fun Number.castTo(type: Type) =
        if (type is Type.Int) {
          toInt()
        } else if (type is Type.Float) {
          toDouble()
        } else {
          this
        }
  }
}
