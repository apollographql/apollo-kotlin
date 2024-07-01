package com.apollographql.apollo.compiler.codegen.java.helpers

import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaContext
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.internal.applyIf
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class BuilderBuilder(
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
          .applyIf(context.resolver.isOptional(it.type)) {
            initializer(context.absentOptionalInitializer())
          }
          .build()
    }
  }

  private fun fieldSetterMethodSpecs(): List<MethodSpec> {
    return fields.map {
      fieldSetterMethodSpec(it)
    }
  }

  private fun fieldSetterMethodSpec(field: FieldSpec): MethodSpec {
    val unwrappedFieldType = context.resolver.unwrapFromOptional(field.type)
    return MethodSpec.methodBuilder(field.name)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(
            ParameterSpec.builder(unwrappedFieldType, field.name)
                .apply {
                  if (context.resolver.isOptional(unwrappedFieldType)) {
                    addAnnotation(context.resolver.notNullAnnotationClassName ?: JavaClassNames.JetBrainsNonNull)
                  } else if (!context.resolver.isOptional(field.type)) {
                    addAnnotations(field.annotations)
                  }
                }
                .build()
        )
        .applyIf(!field.javadoc.isEmpty) {
          addJavadoc(L, field.javadoc)
        }
        .returns(JavaClassNames.Builder)
        .addStatement("this.$L = $L", field.name, context.wrapValueInOptional(value = CodeBlock.of(L, field.name), fieldType = field.type))
        .addStatement("return this")
        .build()
  }

  private fun buildMethod(): MethodSpec {
    return MethodSpec
        .methodBuilder("build")
        .addModifiers(Modifier.PUBLIC)
        .returns(targetObjectClassName)
        .addStatement(
            "return new $T$L",
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
          .addStatement("return new $T()", JavaClassNames.Builder)
          .build()
    }
  }
}
