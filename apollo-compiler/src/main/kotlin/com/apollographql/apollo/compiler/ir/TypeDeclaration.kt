package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.Annotations
import com.apollographql.apollo.compiler.InputTypeSpecBuilder
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class TypeDeclaration(
    val kind: String,
    val name: String,
    val description: String?,
    val values: List<TypeDeclarationValue>?,
    val fields: List<TypeDeclarationField>?
) : CodeGenerator {
  override fun toTypeSpec(context: CodeGenerationContext): TypeSpec {
    if (kind == KIND_ENUM) {
      return enumTypeToTypeSpec()
    } else if (kind == KIND_INPUT_OBJECT_TYPE) {
      return inputObjectToTypeSpec(context)
    } else {
      throw UnsupportedOperationException("unsupported $kind type declaration")
    }
  }

  private fun enumTypeToTypeSpec(): TypeSpec {
    fun TypeSpec.Builder.addEnumJavaDoc(): TypeSpec.Builder {
      if (!description.isNullOrEmpty()) {
        addJavadoc("\$L\n", description)
      }
      return this
    }

    fun TypeSpec.Builder.addEnumConstants(): TypeSpec.Builder {
      values?.forEach { value ->
        if (value.description.isNullOrEmpty()) {
          addEnumConstant(value.name)
              .let {
                if (value.isDeprecated ?: false && !value.deprecationReason.isNullOrBlank()) {
                  it.addJavadoc("@deprecated \$L\n", value.deprecationReason)
                } else {
                  it
                }
              }
              .let { if (value.isDeprecated ?: false) it.addAnnotation(Annotations.DEPRECATED) else it }
        } else {
          addEnumConstant(value.name, TypeSpec.anonymousClassBuilder("")
              .addJavadoc("\$L\n", value.description)
              .let {
                if (value.isDeprecated ?: false && !value.deprecationReason.isNullOrBlank()) {
                  it.addJavadoc("@deprecated \$L\n", value.deprecationReason)
                } else {
                  it
                }
              }
              .let { if (value.isDeprecated ?: false) it.addAnnotation(Annotations.DEPRECATED) else it }
              .build()
          )
        }
      }
      return this
    }

    return TypeSpec.enumBuilder(name)
        .addAnnotation(Annotations.GENERATED_BY_APOLLO)
        .addModifiers(Modifier.PUBLIC)
        .addEnumConstants()
        .addEnumJavaDoc()
        .build()
  }

  private fun inputObjectToTypeSpec(context: CodeGenerationContext) =
      InputTypeSpecBuilder(name, fields ?: emptyList(), context).build()

  companion object {
    val KIND_INPUT_OBJECT_TYPE: String = "InputObjectType"
    val KIND_ENUM: String = "EnumType"
    val KIND_SCALAR_TYPE: String = "ScalarType"
  }
}
