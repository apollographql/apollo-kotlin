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
    if (kind == "EnumType") {
      return enumTypeToTypeSpec()
    } else if (kind == "InputObjectType") {
      return inputObjectToTypeSpec(context)
    } else {
      throw UnsupportedOperationException("unsupported $kind type declaration")
    }
  }

  private fun enumTypeToTypeSpec(): TypeSpec {
    fun TypeSpec.Builder.addTypeDeclarationValue(value: TypeDeclarationValue) {
      val enumConstBuilder = TypeSpec.anonymousClassBuilder("")
      if (!value.description.isNullOrEmpty()) {
        enumConstBuilder.addJavadoc("${value.description}\n")
      }
      this.addEnumConstant(value.name.toUpperCase(), enumConstBuilder.build())
    }

    val builder = TypeSpec.enumBuilder(name).addAnnotation(Annotations.GENERATED_BY_APOLLO)
        .addModifiers(Modifier.PUBLIC)
    values?.forEach { builder.addTypeDeclarationValue(it) }
    if (!description.isNullOrEmpty()) {
      builder.addJavadoc("$description\n")
    }
    return builder.build()
  }

  private fun inputObjectToTypeSpec(context: CodeGenerationContext) =
      InputObjectTypeSpecBuilder(name, fields ?: emptyList(), context).build()
}
