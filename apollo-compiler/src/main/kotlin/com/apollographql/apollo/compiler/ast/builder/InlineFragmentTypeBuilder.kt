package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.ast.FieldType
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.ast.TypeRef
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.InlineFragment

internal fun inlineObjectFields(
    inlineFragments: Map<InlineFragment, TypeRef>,
    isOptional: InlineFragment.() -> Boolean
): List<ObjectType.Field> {
  return inlineFragments.map { (inlineFragment, typeRef) ->
    ObjectType.Field(
        name = typeRef.name.decapitalize().escapeKotlinReservedWord(),
        responseName = "__typename",
        schemaName = "__typename",
        type = FieldType.InlineFragment(typeRef),
        description = "",
        isOptional = inlineFragment.isOptional(),
        isDeprecated = false,
        deprecationReason = "",
        arguments = emptyMap(),
        conditions = inlineFragment.possibleTypes?.map { ObjectType.Field.Condition.Type(it) }
            ?: listOf(ObjectType.Field.Condition.Type(inlineFragment.typeCondition))
    )
  }
}