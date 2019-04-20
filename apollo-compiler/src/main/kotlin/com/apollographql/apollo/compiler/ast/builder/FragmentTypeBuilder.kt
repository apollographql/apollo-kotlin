package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.ast.*
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.Fragment

internal fun Fragment.ast(context: Context): FragmentType {
  val inlineFragmentFields = inlineFragments
      .associate { it to context.registerInlineFragmentType(it) }
      .let { inlineObjectFields(it) { true } }
  return FragmentType(
      name = fragmentName.capitalize().escapeKotlinReservedWord(),
      definition = source,
      possibleTypes = possibleTypes,
      fields = fields.map { it.ast(context) } + inlineFragmentFields,
      nestedObjects = context.objectTypes
  )
}

internal fun List<Fragment>.astObjectFieldType(
    fragmentsPackage: String,
    isOptional: Fragment.() -> Boolean
): Pair<ObjectType.Field?, ObjectType?> {
  if (isEmpty()) {
    return null to null
  }
  val type = ObjectType(
      className = "Fragments",
      schemaName = "Fragments",
      fields = map { fragment ->
        ObjectType.Field(
            name = fragment.fragmentName.decapitalize().escapeKotlinReservedWord(),
            responseName = fragment.fragmentName,
            schemaName = fragment.fragmentName,
            type = FieldType.Object(TypeRef(
                name = fragment.fragmentName.capitalize(),
                packageName = fragmentsPackage
            )),
            description = "",
            isOptional = fragment.isOptional(),
            isDeprecated = false,
            deprecationReason = "",
            arguments = emptyMap(),
            conditions = fragment.possibleTypes.map {
              ObjectType.Field.Condition.Type(it)
            }
        )
      },
      fragmentsType = null
  )
  val field = ObjectType.Field(
      name = type.className.decapitalize().escapeKotlinReservedWord(),
      responseName = "__typename",
      schemaName = "__typename",
      type = FieldType.Fragments(
          name = type.className,
          fields = type.fields.map { field ->
            FieldType.Fragments.Field(
                name = field.name,
                type = (field.type as FieldType.Object).typeRef,
                isOptional = field.isOptional
            )
          }
      ),
      description = "",
      isOptional = false,
      isDeprecated = false,
      deprecationReason = "",
      arguments = emptyMap(),
      conditions = emptyList()
  )
  return field to type
}