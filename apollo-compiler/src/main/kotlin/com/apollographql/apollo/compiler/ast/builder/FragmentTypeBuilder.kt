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
      schemaTypeName = typeCondition,
      description = "",
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
    parentFieldSchemaTypeName: String
): Pair<ObjectType.Field?, ObjectType?> {
  if (isEmpty()) {
    return null to null
  }
  val type = ObjectType(
      name = "Fragments",
      schemaTypeName = "",
      description = "",
      fields = map { (fragmentRef, fragment) ->
        val isOptional = fragmentRef.conditions.isNotEmpty() || fragment.typeCondition != parentFieldSchemaTypeName
        val possibleTypes = fragment.takeIf { fragment.typeCondition != parentFieldSchemaTypeName }?.possibleTypes ?: emptyList()
        ObjectType.Field(
            name = fragment.fragmentName.decapitalize().escapeKotlinReservedWord(),
            responseName = "__typename",
            schemaName = "__typename",
            type = FieldType.Fragment(TypeRef(
                name = fragment.fragmentName.capitalize(),
                packageName = fragmentsPackage
            )),
            description = "",
            isOptional = isOptional,
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
                }.let { conditions ->
                  if (possibleTypes.isNotEmpty()) {
                    conditions + ObjectType.Field.Condition.Type(possibleTypes)
                  } else {
                    conditions
                  }
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
