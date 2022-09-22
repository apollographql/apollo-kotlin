package com.apollographql.apollo3.compiler.codegen.java.helpers

import com.apollographql.apollo3.compiler.JavaNullableFieldStyle
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.ClassNames
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
    val fields: List<Pair<String, TypeName>>,
    val fieldJavaDocs: Map<String, String>,
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
    return fields.map { (fieldName, fieldType) ->
      FieldSpec.builder(fieldType, fieldName)
          .addModifiers(Modifier.PRIVATE)
          .build()
    }
  }

  private fun fieldSetterMethodSpecs(): List<MethodSpec> {
    return fields.map { (fieldName, fieldType) ->
      val javaDoc = fieldJavaDocs[fieldName]
      fieldSetterMethodSpec(fieldName, fieldType, javaDoc)
    }
  }

  private fun fieldSetterMethodSpec(fieldName: String, fieldType: TypeName, javaDoc: String?): MethodSpec {
    return MethodSpec.methodBuilder(fieldName)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(
            ParameterSpec.builder(context.resolver.unwrapFromOptional(fieldType), fieldName)
                .applyIf(context.resolver.isOptional(fieldType)) {
                  // TODO: Other flavors of Nullable annotations will be supported later
                  addAnnotation(JavaClassNames.JetBrainsNullable)
                }
                .build()
        )
        .apply {
          if (!javaDoc.isNullOrBlank()) {
            addJavadoc(CodeBlock.of("\$L\n", javaDoc))
          }
        }
        .returns(JavaClassNames.Builder)
        .addStatement("this.\$L = \$L", fieldName, wrapValueInOptional(fieldName, fieldType, context.nullableFieldStyle))
        .addStatement("return this")
        .build()
  }

  private fun wrapValueInOptional(value: String, fieldType: TypeName, nullableFieldStyle: JavaNullableFieldStyle): CodeBlock {
    val valueCodeBlock = CodeBlock.of(L, value)
    return if (!context.resolver.isOptional(fieldType)) {
      valueCodeBlock
    } else {
      when (nullableFieldStyle) {
        JavaNullableFieldStyle.JAVA_OPTIONAL -> CodeBlock.of("\$T.ofNullable(\$L)", JavaClassNames.JavaOptional, value)
        JavaNullableFieldStyle.GUAVA_OPTIONAL -> CodeBlock.of("\$T.fromNullable(\$L)", JavaClassNames.GuavaOptional, value)
        else -> CodeBlock.of("\$T.presentIfNotNull(\$L)", JavaClassNames.Optional, value)
      }
    }
  }

  private fun buildMethod(): MethodSpec {
    val validationCodeBuilder = fields.filter { (_, fieldType) ->
      !fieldType.isPrimitive && fieldType.annotations.contains(AnnotationSpec.builder(JavaClassNames.JetBrainsNonNull).build())
    }.map { (fieldName, _) ->
      CodeBlock.of("\$T.checkFieldNotMissing(\$L, \$S);\n", ClassNames.Assertions, fieldName, fieldName)
    }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

    return MethodSpec
        .methodBuilder("build")
        .addModifiers(Modifier.PUBLIC)
        .returns(targetObjectClassName)
        .addCode(validationCodeBuilder.build())
        .addStatement(
            "return new \$T\$L",
            targetObjectClassName,
            fields.joinToString(prefix = "(", separator = ", ", postfix = ")") { it.first }
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
