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
        val typeSpec = TypeSpec.anonymousClassBuilder("")
            .apply {
              if (!value.description.isNullOrEmpty()) {
                addJavadoc("\$L\n", value.description)
              }
            }
            .apply {
              if (value.isDeprecated == true && !value.deprecationReason.isNullOrBlank()) {
                addJavadoc("@deprecated \$L\n", value.deprecationReason)
              }
            }
            .apply {
              if (value.isDeprecated == true) {
                addAnnotation(Annotations.DEPRECATED)
              }
            }
            .build()
        addEnumConstant(value.name, typeSpec)
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
