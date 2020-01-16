package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.ast.FieldType
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.ast.TypeRef
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.Condition
import com.apollographql.apollo.compiler.ir.Fragment
import com.apollographql.apollo.compiler.ir.FragmentRef

internal fun Fragment.ast(context: Context): ObjectType {
  val typeRef = context.registerObjectType(
      name = fragmentName.capitalize().escapeKotlinReservedWord(),
      schemaTypeName = "",
      fragmentRefs = fragmentRefs,
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

internal fun Map<FragmentRef, Fragment>.astFragmentsObjectFieldType(
    fragmentsPackage: String,
    isOptional: Fragment.() -> Boolean
): Pair<ObjectType.Field?, ObjectType?> {
  if (isEmpty()) {
    return null to null
  }
  val type = ObjectType(
      name = "Fragments",
      schemaTypeName = "",
      fields = map { (fragmentRef, fragment) ->
        ObjectType.Field(
            name = fragment.fragmentName.decapitalize().escapeKotlinReservedWord(),
            responseName = "__typename",
            schemaName = "__typename",
            type = FieldType.Fragment(TypeRef(
                name = fragment.fragmentName.capitalize(),
                packageName = fragmentsPackage
            )),
            description = "",
            isOptional = fragmentRef.conditions.isNotEmpty() || fragment.isOptional(),
            isDeprecated = false,
            deprecationReason = "",
            arguments = emptyMap(),
            conditions = fragmentRef.conditions
                .filter { condition -> condition.kind == Condition.Kind.BOOLEAN.rawValue }
                .map { condition ->
                  ObjectType.Field.Condition.Directive(
                      variableName = condition.variableName,
                      inverted = condition.inverted
                  )
                } + ObjectType.Field.Condition.Type(fragment.possibleTypes)
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
