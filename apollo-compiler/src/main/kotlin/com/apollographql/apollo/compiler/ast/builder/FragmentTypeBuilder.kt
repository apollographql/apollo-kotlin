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
  val inlineFragmentFields = if (inlineFragments.isNotEmpty()) {
    val inlineFragmentSuper = context.registerInlineFragmentSuper(
        type = fragmentName,
        schemaType = typeCondition
    )
    inlineFragments.map {
      it.inlineFragmentField(
          context = context,
          fragmentSuper = inlineFragmentSuper
      )
    }
  } else emptyList()
  val nestedObjects = context.minus(typeRef)
  return context[typeRef]!!.run {
    copy(
        fields = fields + inlineFragmentFields,
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
            responseName = "__typename",
            schemaName = "__typename",
            type = FieldType.Fragment(TypeRef(
                name = fragment.fragmentName.capitalize(),
                packageName = fragmentsPackage
            )),
            description = "",
            isOptional = fragment.isOptional(),
            isDeprecated = false,
            deprecationReason = "",
            arguments = emptyMap(),
            conditions = fragment.possibleTypes.map {
              ObjectType.Field.Condition.Type(listOf(it))
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
                type = (field.type as FieldType.Fragment).typeRef,
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
