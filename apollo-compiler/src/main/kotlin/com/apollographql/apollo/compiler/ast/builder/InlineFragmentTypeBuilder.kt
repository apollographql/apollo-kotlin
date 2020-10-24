package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.ast.FieldType
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.ast.TypeRef
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.Condition
import com.apollographql.apollo.compiler.ir.InlineFragment
import com.apollographql.apollo.compiler.singularize

internal fun Context.registerInlineFragmentSuper(type: String, schemaType: String): TypeRef {
  val superInterfaceName = type.replace("[", "").replace("]", "").replace("!", "").singularize().capitalize() +
      schemaType.replace("[", "").replace("]", "").replace("!", "").singularize().capitalize()
  return registerObjectType(
      name = superInterfaceName.escapeKotlinReservedWord(),
      schemaTypeName = "",
      description = "",
      fragmentRefs = emptyList(),
      inlineFragments = emptyList(),
      fields = emptyList(),
      kind = ObjectType.Kind.InlineFragmentSuper,
      singularize = false
  )
}

internal fun InlineFragment.inlineFragmentField(
    context: Context,
    fragmentSuper: TypeRef
): ObjectType.Field {
  val normalizedClassName = typeCondition.capitalize().escapeKotlinReservedWord()
  val typeRef = context.registerObjectType(
      name = "As$normalizedClassName",
      schemaTypeName = typeCondition,
      description = description,
      fragmentRefs = fragments,
      inlineFragments = inlineFragments,
      fields = fields,
      kind = ObjectType.Kind.InlineFragment(
          superInterface = fragmentSuper,
          possibleTypes = possibleTypes
      ),
      singularize = false
  )
  return ObjectType.Field(
      name = typeRef.name.decapitalize(),
      responseName = "__typename",
      schemaName = "__typename",
      type = FieldType.Fragment(typeRef),
      description = "",
      isOptional = true,
      deprecationReason = null,
      arguments = emptyMap(),
      conditions = conditions.filter { it.kind == Condition.Kind.BOOLEAN.rawValue }.map { directive ->
        ObjectType.Field.Condition.Directive(variableName = directive.variableName, inverted = directive.inverted)
      } + listOf(ObjectType.Field.Condition.Type(possibleTypes))
  )
}
