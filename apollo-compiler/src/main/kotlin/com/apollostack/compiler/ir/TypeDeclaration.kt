package com.apollostack.compiler.ir

import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class TypeDeclaration(
    val kind: String,
    val name: String,
    val description: String?,
    val values: List<TypeDeclarationValue>?
) : CodeGenerator {
  override fun toTypeSpec(): TypeSpec {
    if (kind == "EnumType") {
      return enumTypeToTypeSpec()
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

    val builder = TypeSpec.enumBuilder(name).addModifiers(Modifier.PUBLIC)
    values?.forEach { builder.addTypeDeclarationValue(it) }
    if (!description.isNullOrEmpty()) {
      builder.addJavadoc("$description\n")
    }
    return builder.build()
  }
}