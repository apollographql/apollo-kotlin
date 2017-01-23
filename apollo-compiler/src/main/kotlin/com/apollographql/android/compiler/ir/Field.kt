package com.apollographql.android.compiler.ir

import com.apollographql.android.compiler.JavaTypeResolver
import com.apollographql.android.compiler.SchemaTypeSpecBuilder
import com.cesarferreira.pluralize.singularize
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class Field(
    val responseName: String,
    val fieldName: String,
    val type: String,
    val isConditional: Boolean = false,
    val fields: List<Field>?,
    val fragmentSpreads: List<String>?,
    val inlineFragments: List<InlineFragment>?
) : CodeGenerator {
  override fun toTypeSpec(abstractClass: Boolean, reservedTypeNames: List<String>,
      typeDeclarations: List<TypeDeclaration>, fragmentsPackage: String, typesPackage: String): TypeSpec =
      SchemaTypeSpecBuilder(normalizedName(), fields ?: emptyList(), fragmentSpreads ?: emptyList(),
          inlineFragments ?: emptyList(), abstractClass, reservedTypeNames, typeDeclarations, fragmentsPackage,
          typesPackage)
          .build(Modifier.PUBLIC, Modifier.STATIC)

  fun accessorMethodSpec(abstract: Boolean, typesPackage: String = ""): MethodSpec {
    val methodSpecBuilder = MethodSpec
        .methodBuilder(responseName)
        .addModifiers(Modifier.PUBLIC)
        .addModifiers(if (abstract) listOf(Modifier.ABSTRACT) else emptyList())
        .returns(toTypeName(methodResponseType(), typesPackage))
    if (!abstract) {
      methodSpecBuilder.addStatement("return this.\$L", responseName)
    }
    return methodSpecBuilder.build()
  }

  fun fieldSpec(typesPackage: String = ""): FieldSpec = FieldSpec
      .builder(toTypeName(methodResponseType(), typesPackage), responseName)
      .addModifiers(Modifier.PRIVATE)
      .build()

  private fun toTypeName(responseType: String, typesPackage: String): TypeName {
    val packageName = if (fields?.any() ?: false || hasFragments()) "" else typesPackage
    return JavaTypeResolver(packageName).resolve(responseType, isOptional())
  }

  fun normalizedName() = responseName.capitalize().singularize()

  private fun methodResponseType(): String {
    if (isNonScalar() || hasFragments()) {
      // For non scalar fields, we use the responseName as the method return type.
      // However, we need to also encode any extra information from the `type` field
      // eg, [lists], nonNulls!, [[nestedLists]], [nonNullLists]!, etc
      val normalizedName = normalizedName()
      if (type.startsWith("[")) {
        // array type
        return if (type.endsWith("!")) "[$normalizedName]!" else "[$normalizedName]"
      } else if (type.endsWith("!")) {
        // non-null type
        return "$normalizedName!"
      } else {
        // nullable type
        return normalizedName
      }
    } else {
      return type
    }
  }

  fun isNonScalar() = hasFragments() || (fields?.any() ?: false)

  fun hasFragments() = fragmentSpreads?.any() ?: false

  fun isOptional(): Boolean = isConditional || !methodResponseType().endsWith("!")
}
