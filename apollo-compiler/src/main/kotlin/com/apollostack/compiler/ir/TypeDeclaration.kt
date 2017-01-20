package com.apollostack.compiler.ir

import com.apollostack.compiler.InputObjectTypeSpecBuilder
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

data class TypeDeclaration(
    val kind: String,
    val name: String,
    val description: String?,
    val values: List<TypeDeclarationValue>?,
    val fields: List<TypeDeclarationField>?,
    val fragmentsPkgName: String,
    val typesPkgName: String
) : CodeGenerator {
  override fun toTypeSpec(abstractClass: Boolean, reservedTypeNames: List<String>,
      typeDeclarations: List<TypeDeclaration>, fragmentsPkgName: String, typesPkgName: String): TypeSpec {
    if (kind == "EnumType") {
      return enumTypeToTypeSpec()
    } else if (kind == "InputObjectType") {
      return inputObjectToTypeSpec(typesPkgName)
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

  private fun inputObjectToTypeSpec(typesPkgName: String) =
      InputObjectTypeSpecBuilder(name, fields ?: emptyList(), typesPkgName).build()
}
