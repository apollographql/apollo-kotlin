package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.TypeDeclaration
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class BuilderTypeSpecBuilder(
    val targetObjectClassName: ClassName,
    val fields: List<Pair<String, TypeName>>,
    val fieldDefaultValues: Map<String, Any?>,
    val typeDeclarations: List<TypeDeclaration>
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
        val defaultValue = fieldDefaultValues[fieldName]?.let {
          (it as? Number)?.castTo(fieldType.withoutAnnotations()) ?: it
        }
        val initializerCode = defaultValue?.let {
          if (fieldType.isEnum(typeDeclarations))
            CodeBlock.of("\$T.\$L", fieldType.withoutAnnotations(), defaultValue)
          else
            CodeBlock.of("\$L", defaultValue)
        } ?: CodeBlock.of("")
        FieldSpec.builder(fieldType, fieldName)
            .addModifiers(Modifier.PRIVATE)
            .initializer(initializerCode)
            .build()
      })

  private fun TypeSpec.Builder.addBuilderMethods(): TypeSpec.Builder =
      addMethods(fields.map {
        val fieldName = it.first
        val fieldType = it.second
        MethodSpec.methodBuilder(fieldName)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ParameterSpec.builder(fieldType, fieldName).build())
            .returns(builderClassName)
            .addStatement("this.\$L = \$L", fieldName, fieldName)
            .addStatement("return this")
            .build()
      })

  private fun TypeSpec.Builder.addBuilderBuildMethod(): TypeSpec.Builder {
    val validationCodeBuilder = fields.filter {
      val fieldType = it.second
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

    private fun Number.castTo(type: TypeName) =
        if (type == TypeName.INT || type == TypeName.INT.box()) {
          toInt()
        } else if (type == TypeName.FLOAT || type == TypeName.FLOAT.box()) {
          toDouble()
        } else {
          this
        }

    private fun TypeName.isEnum(typeDeclarations: List<TypeDeclaration>) =
        ((this is ClassName) && typeDeclarations.count { it.kind == "EnumType" && it.name == simpleName() } > 0)
  }
}
