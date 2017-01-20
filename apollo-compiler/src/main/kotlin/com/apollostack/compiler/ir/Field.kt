package com.apollostack.compiler.ir

import com.apollostack.compiler.SchemaTypeSpecBuilder
import com.apollostack.compiler.ir.graphql.Type
import com.cesarferreira.pluralize.singularize
import com.squareup.javapoet.*
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
      typeDeclarations: List<TypeDeclaration>, fragmentsPkgName: String, typesPkgName: String): TypeSpec =
      SchemaTypeSpecBuilder(normalizedName(), fields ?: emptyList(), fragmentSpreads ?: emptyList(),
          inlineFragments ?: emptyList(), abstractClass, reservedTypeNames, typeDeclarations, fragmentsPkgName, typesPkgName)
          .build(Modifier.PUBLIC, Modifier.STATIC)

  fun accessorMethodSpec(abstract: Boolean, typesPkgName: String = ""): MethodSpec {
    val methodSpecBuilder = MethodSpec
        .methodBuilder(responseName)
        .addModifiers(Modifier.PUBLIC)
        .addModifiers(if (abstract) listOf(Modifier.ABSTRACT) else emptyList())
        .returns(toTypeName(methodResponseType(), typesPkgName))
    if (!abstract) {
      methodSpecBuilder.addStatement("return this.\$L", responseName)
    }
    return methodSpecBuilder.build()
  }

  fun fieldSpec(typesPkgName: String = ""): FieldSpec = FieldSpec
      .builder(toTypeName(methodResponseType(), typesPkgName), responseName)
      .addModifiers(Modifier.PRIVATE)
      .build()

  private fun toTypeName(responseType: String, typesPkgName: String): TypeName =
      Type.resolveByName(responseType, isOptional())
          .toJavaTypeName(if (fields?.any() ?: false || hasFragments()) "" else typesPkgName)

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
