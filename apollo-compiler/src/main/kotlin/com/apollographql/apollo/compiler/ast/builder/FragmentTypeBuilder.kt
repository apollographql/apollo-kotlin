package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.ast.FieldType
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.ast.TypeRef
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.Fragment

internal fun Fragment.ast(context: Context): ObjectType {
  val typeRef = context.registerObjectType(
      name = fragmentName.capitalize().escapeKotlinReservedWord(),
      schemaTypeName = "",
      fragmentSpreads = fragmentSpreads,
      inlineFragments = emptyList(),
      fields = fields,
      singularize = false,
      kind = ObjectType.Kind.Fragment(
          definition = source,
          possibleTypes = possibleTypes
      )
  )
  val inlineFragmentField = inlineFragments.takeIf { it.isNotEmpty() }?.inlineFragmentField(
      type = fragmentName,
      schemaType = typeCondition,
      context = context
  )
  val nestedObjects = context.minus(typeRef)
  return context[typeRef]!!.run {
    copy(
        fields = fields.let { if (inlineFragmentField != null) it + inlineFragmentField else it },
        nestedObjects = nestedObjects
    )
  }
}

internal fun List<Fragment>.astFragmentsObjectFieldType(
    fragmentsPackage: String,
    isOptional: Fragment.() -> Boolean
): Pair<ObjectType.Field?, ObjectType?> {
  if (isEmpty()) {
    return null to null
  }
  val type = ObjectType(
      name = "Fragments",
      schemaTypeName = "",
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
      fragmentsType = null,
      kind = ObjectType.Kind.Object
  )
  val field = ObjectType.Field(
      name = type.name.decapitalize().escapeKotlinReservedWord(),
      responseName = "__typename",
      schemaName = "__typename",
      type = FieldType.Fragments(
          name = type.name,
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
