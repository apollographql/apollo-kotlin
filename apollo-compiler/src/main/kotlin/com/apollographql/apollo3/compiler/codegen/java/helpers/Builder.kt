package com.apollographql.apollo3.compiler.codegen.java.helpers

import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class Builder(
    val targetObjectClassName: ClassName,
    val fields: List<FieldSpec>,
    val context: JavaContext,
) {
  fun build(): TypeSpec {
    return TypeSpec.classBuilder(JavaClassNames.Builder.simpleName())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addFields(builderFields())
        .addMethod(MethodSpec.constructorBuilder().build())
        .addMethods(fieldSetterMethodSpecs())
        .addMethod(buildMethod())
        .build()
  }

  private fun builderFields(): List<FieldSpec> {
    return fields.map {
      FieldSpec.builder(it.type, it.name, Modifier.PRIVATE)

          .build()
    }
  }

  private fun fieldSetterMethodSpecs(): List<MethodSpec> {
    return fields.map {
      fieldSetterMethodSpec(it)
    }
  }

  private fun fieldSetterMethodSpec(field: FieldSpec): MethodSpec {
    return MethodSpec.methodBuilder(field.name)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(
            ParameterSpec.builder(context.resolver.unwrapFromOptional(field.type), field.name)
                .apply {
                  if (context.resolver.isOptional(field.type)) {
                    // TODO: Other flavors of Nullable annotations will be supported later
                    addAnnotation(JavaClassNames.JetBrainsNullable)
                  } else {
                    addAnnotations(field.annotations)
                  }
                }
                .build()
        )
        .addJavadoc(field.javadoc)
        .returns(JavaClassNames.Builder)
        .addStatement("this.\$L = \$L", field.name, wrapValueInOptional(field.name, field.type, context.nullableFieldStyle))
        .addStatement("return this")
        .build()
  }

  private fun wrapValueInOptional(value: String, fieldType: TypeName, nullableFieldStyle: JavaNullable): CodeBlock {
    return if (!context.resolver.isOptional(fieldType)) {
      CodeBlock.of(L, value)
    } else {
      when (nullableFieldStyle) {
        JavaNullable.JAVA_OPTIONAL -> CodeBlock.of("\$T.ofNullable(\$L)", JavaClassNames.JavaOptional, value)
        JavaNullable.GUAVA_OPTIONAL -> CodeBlock.of("\$T.fromNullable(\$L)", JavaClassNames.GuavaOptional, value)
        else -> CodeBlock.of("\$T.presentIfNotNull(\$L)", JavaClassNames.Optional, value)
      }
    }
  }

  private fun buildMethod(): MethodSpec {
    val validationCodeBuilder = fields.filter {
      // TODO Hardcoded annotation type
      !it.type.isPrimitive && it.annotations.contains(AnnotationSpec.builder(JavaClassNames.JetBrainsNonNull).build())
    }.map {
      CodeBlock.of("\$T.checkFieldNotMissing(\$L, \$S);\n", JavaClassNames.Assertions, it.name, it.name)
    }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

    return MethodSpec
        .methodBuilder("build")
        .addModifiers(Modifier.PUBLIC)
        .returns(targetObjectClassName)
        .addCode(validationCodeBuilder.build())
        .addStatement(
            "return new \$T\$L",
            targetObjectClassName,
            fields.joinToString(prefix = "(", separator = ", ", postfix = ")") { it.name }
        )
        .build()
  }

  companion object {
    const val TO_BUILDER_METHOD_NAME = "toBuilder"

    fun builderFactoryMethod(): MethodSpec {
      return MethodSpec
          .methodBuilder("builder")
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .returns(JavaClassNames.Builder)
          .addStatement("return new \$T()", JavaClassNames.Builder)
          .build()
    }
  }
}
