package com.apollostack.compiler.ir

import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class TypeDeclaration(
    val kind: String,
    val name: String,
    val values: List<TypeDeclarationValue>?) {

  fun toTypeSpec(): TypeSpec {
    if (kind == "EnumType") {
      val builder = TypeSpec.enumBuilder(name).addModifiers(Modifier.PUBLIC)
      values?.forEach {
        builder.addEnumConstant(it.name.toUpperCase())
      }
      return builder.build()
    } else {
      throw UnsupportedOperationException("unsupported $kind type declaration")
    }
  }
}