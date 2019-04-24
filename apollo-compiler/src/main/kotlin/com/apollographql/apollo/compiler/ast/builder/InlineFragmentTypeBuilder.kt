package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.ast.FieldType
import com.apollographql.apollo.compiler.ast.InlineFragmentRef
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.InlineFragment
import com.apollographql.apollo.compiler.singularize

internal fun List<InlineFragment>.inlineFragmentField(
    type: String,
    schemaType: String,
    context: Context
): ObjectType.Field {
  val superTypeName = type.replace("[", "").replace("]", "").replace("!", "").singularize().capitalize() +
      schemaType.replace("[", "").replace("]", "").replace("!", "").singularize().capitalize()
  val superType = context.addObjectType(superTypeName.escapeKotlinReservedWord()) { typeRef ->
    ObjectType(
        className = typeRef.name,
        schemaName = "",
        fields = emptyList(),
        fragmentsType = null,
        abstract = true
    )
  }
  val inlineFragmentRefs = associate { fragment ->
    context.registerObjectType(
        type = "As${fragment.typeCondition}",
        schemaType = fragment.typeCondition,
        fragmentSpreads = fragment.fragmentSpreads ?: emptyList(),
        inlineFragments = emptyList(),
        fields = fragment.fields,
        superType = superType
    ) to (fragment.possibleTypes ?: listOf(fragment.typeCondition))
  }
      .map { (typeRef, possibleTypes) -> InlineFragmentRef(typeRef, possibleTypes) }
  return ObjectType.Field(
      name = "inlineFragment",
      responseName = "__typename",
      schemaName = "__typename",
      type = FieldType.InlineFragment(superType, inlineFragmentRefs),
      description = "",
      isOptional = true,
      isDeprecated = false,
      deprecationReason = "",
      arguments = emptyMap(),
      conditions = flatMap { inlineFragment ->
        inlineFragment.possibleTypes?.map { ObjectType.Field.Condition.Type(it) }
            ?: listOf(ObjectType.Field.Condition.Type(inlineFragment.typeCondition))
      }
  )
}