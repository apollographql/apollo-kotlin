package com.apollographql.apollo.compiler.next.ast

internal fun ObjectTypeContainer.patchTypeHierarchy(fragmentTypes: List<CodeGenerationAst.FragmentType>): ObjectTypeContainer {
  val fragmentTypeContainer = fragmentTypes.fold(emptyMap<CodeGenerationAst.TypeRef, CodeGenerationAst.ObjectType>()) { acc, fragmentType ->
    acc + fragmentType.nestedTypes
  }
  return TypeHierarchyPatcher(this + fragmentTypeContainer)
      .patch()
      .minus(fragmentTypeContainer.keys)
}

private class TypeHierarchyPatcher(typeContainer: ObjectTypeContainer) {
  private val patchedTypeContainer = typeContainer.toMutableMap()
  private val patchedTypes = mutableListOf<CodeGenerationAst.TypeRef>()

  fun patch(): Map<CodeGenerationAst.TypeRef, CodeGenerationAst.ObjectType> {
    patchedTypeContainer.keys.forEach { typeRef ->
      typeRef.takeUnless { patchedTypes.contains(it) }?.patch()
    }
    return patchedTypeContainer
  }

  private fun CodeGenerationAst.TypeRef.patch() {
    patchedTypes.add(this)

    val type = requireNotNull(patchedTypeContainer[this]) {
      "Can't resolve type, unknown type reference `$this`"
    }

    type.implements.forEach { interfaceType -> interfaceType.patch() }

    val inheritedFields = type.implements.flatMap { parentType ->
      (patchedTypeContainer[parentType] as CodeGenerationAst.ObjectType).fields
    }

    val (newFields, parentFields) = inheritedFields.partition { parentTypeField ->
      type.fields.find { field -> field.name == parentTypeField.name } == null
    }

    val patchedFields = type.fields.map { field ->
      val parentField = parentFields.find { parentField ->
        parentField.name == field.name
      }

      check(parentField == null || parentField.type.nullable || !field.type.nullable) {
        "Nullable field `${field.responseName}` of type `${field.type}` defined on object `${name}` is not a subtype of non nullable " +
            "overridden field. Please use field alias instead."
      }

      field.takeIf { parentField == null } ?: field.copy(override = true)
    }

    patchedTypeContainer[this] = type.copy(fields = newFields.map { it.copy(override = true) } + patchedFields)

    patchedFields.forEach { field ->
      val parentFieldTypes = parentFields.mapNotNull { parentField ->
        parentField.takeIf { it.name == field.name }?.type
      }
      if (parentFieldTypes.isNotEmpty()) {
        field.type.patch(parentFieldTypes)
      }
    }
  }

  private fun CodeGenerationAst.FieldType.patch(parentFieldTypes: List<CodeGenerationAst.FieldType>) {
    when (this) {
      is CodeGenerationAst.FieldType.Object -> {
        parentFieldTypes.forEach { parentFieldType ->
          if (parentFieldType is CodeGenerationAst.FieldType.Object) {
            patch(parentFieldType)
          }
        }
      }

      is CodeGenerationAst.FieldType.Array -> {
        parentFieldTypes.forEach { parentFieldType ->
          patch(parentFieldType as CodeGenerationAst.FieldType.Array)
        }
      }
    }
  }

  private fun CodeGenerationAst.FieldType.Object.patch(
      parentFieldType: CodeGenerationAst.FieldType.Object
  ) {
    val objectTypeToPatch = patchedTypeContainer[typeRef] as CodeGenerationAst.ObjectType
    patchedTypeContainer[typeRef] = objectTypeToPatch.copy(implements = objectTypeToPatch.implements + parentFieldType.typeRef)

    if (objectTypeToPatch.kind is CodeGenerationAst.ObjectType.Kind.Fragment) {
      objectTypeToPatch.kind.defaultImplementation.patch()
      objectTypeToPatch.kind.possibleImplementations.values.forEach { typeRef ->
        patchedTypeContainer[typeRef] = with(patchedTypeContainer[typeRef] as CodeGenerationAst.ObjectType) {
          copy(implements = implements + parentFieldType.typeRef)
        }
        typeRef.patch()
      }
    }

    typeRef.patch()
  }

  private fun CodeGenerationAst.FieldType.Array.patch(
      parentFieldType: CodeGenerationAst.FieldType.Array
  ) {
    when (rawType) {
      is CodeGenerationAst.FieldType.Object -> {
        if (parentFieldType.rawType is CodeGenerationAst.FieldType.Object) {
          rawType.patch(
              parentFieldType = parentFieldType.rawType
          )
        }
      }
      is CodeGenerationAst.FieldType.Array -> rawType.patch(
          parentFieldType = parentFieldType.rawType as CodeGenerationAst.FieldType.Array
      )
    }
  }
}
