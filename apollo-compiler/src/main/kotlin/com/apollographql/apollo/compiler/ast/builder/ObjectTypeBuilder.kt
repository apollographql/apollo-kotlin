package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.ast.*
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.InlineFragment

internal fun Context.registerObjectType(
    type: String,
    schemaType: String,
    fragmentSpreads: List<String>,
    inlineFragments: List<InlineFragment>,
    fields: List<Field>
): TypeRef {
  val (fragmentsField, fragmentsObjectType) = fragmentSpreads
      .map { fragments[it] ?: throw IllegalArgumentException("Unable to find fragment definition: $it") }
      .astObjectFieldType(
          fragmentsPackage = fragmentsPackage,
          isOptional = { typeCondition != schemaType.removeSuffix("!") }
      )

  val inlineFragmentFields = inlineFragments
      .associate { it to registerInlineFragmentType(it) }
      .let { inlineObjectFields(inlineFragments = it) { typeCondition != schemaType.removeSuffix("!") } }

  val normalizedClassName = type.removeSuffix("!").capitalize().escapeKotlinReservedWord()
  return addObjectType(normalizedClassName) { typeRef ->
    ObjectType(
        className = typeRef.name,
        schemaName = type,
        fields = (fields.map { it.ast(this) } + inlineFragmentFields).let {
          if (fragmentsField != null) it + fragmentsField else it
        },
        fragmentsType = fragmentsObjectType
    )
  }
}