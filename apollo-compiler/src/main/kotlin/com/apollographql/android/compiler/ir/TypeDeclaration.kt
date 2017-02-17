package com.apollographql.android.compiler.ir

import com.apollographql.android.compiler.Annotations
import com.apollographql.android.compiler.InputObjectTypeSpecBuilder
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
      values?.forEach {
        if (it.description.isNullOrEmpty()) {
          addEnumConstant(it.name)
        } else {
          addEnumConstant(it.name, TypeSpec.anonymousClassBuilder("")
              .addJavadoc("\$L\n", it.description)
              .build())
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
      InputObjectTypeSpecBuilder(name, fields ?: emptyList(), context).build()

  companion object {
    val KIND_INPUT_OBJECT_TYPE : String = "InputObjectType"
    val KIND_ENUM : String = "EnumType"
    val KIND_SCALAR_TYPE : String = "ScalarType"
  }
}
