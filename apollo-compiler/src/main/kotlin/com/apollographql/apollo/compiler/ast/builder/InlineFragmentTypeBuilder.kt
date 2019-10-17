package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.ast.FieldType
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.InlineFragment
import com.apollographql.apollo.compiler.singularize

internal fun List<InlineFragment>.inlineFragmentField(
    type: String,
    schemaType: String,
    context: Context
): ObjectType.Field {
  val superInterfaceName = type.replace("[", "").replace("]", "").replace("!", "").singularize().capitalize() +
      schemaType.replace("[", "").replace("]", "").replace("!", "").singularize().capitalize()
  val superInterface = context.registerObjectType(
      name = superInterfaceName.escapeKotlinReservedWord(),
      schemaTypeName = "",
      fragmentSpreads = emptyList(),
      inlineFragments = emptyList(),
      fields = emptyList(),
      kind = ObjectType.Kind.InlineFragmentSuper,
      singularize = false
  )
  val inlineFragmentRefs = associate { fragment ->
    val normalizedClassName = fragment.typeCondition.capitalize().escapeKotlinReservedWord()
    val possibleTypes = fragment.possibleTypes

    context.registerObjectType(
        name = "As$normalizedClassName",
        schemaTypeName = fragment.typeCondition,
        fragmentSpreads = fragment.fragmentSpreads,
        inlineFragments = emptyList(),
        fields = fragment.fields,
        kind = ObjectType.Kind.InlineFragment(
            superInterface = superInterface,
            possibleTypes = possibleTypes
        ),
        singularize = false
    ) to possibleTypes
  }
  return ObjectType.Field(
      name = "inlineFragment",
      responseName = "__typename",
      schemaName = "__typename",
      type = FieldType.InlineFragment(superInterface, inlineFragmentRefs.keys.toList()),
      description = "",
      isOptional = true,
      isDeprecated = false,
      deprecationReason = "",
      arguments = emptyMap(),
      conditions = inlineFragmentRefs.values.flatten().map {
        ObjectType.Field.Condition.Type(it)
      }
  )
}
