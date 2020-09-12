package com.apollographql.apollo.compiler.next.ast

internal fun ObjectTypeContainer.patchTypeHierarchy(fragmentTypes: List<CodeGenerationAst.FragmentType>): ObjectTypeContainer {
  val fragmentTypeContainer = fragmentTypes.fold(emptyMap<CodeGenerationAst.TypeRef, CodeGenerationAst.ObjectType>()) { acc, fragmentType ->
    acc + fragmentType.nestedTypes
  }
  return TypeHierarchyPatcher(typeContainer = this, fragmentTypeContainer = fragmentTypeContainer).patch()
}

private class TypeHierarchyPatcher(typeContainer: ObjectTypeContainer, private val fragmentTypeContainer: ObjectTypeContainer) {
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

    val type = patchedTypeContainer[this] ?: return

    // patch all interfaces first
    type.implements.forEach { interfaceType -> interfaceType.patch() }

    // collect all fields from the all interfaces
    val inheritedFields = type.implements.collectFields()

    // set override flag for any existing field from collected interfaces fields
    val mergedFields = type.fields.map { field ->
      val parentField = inheritedFields.find { inheritedField -> inheritedField.name == field.name }
      field.takeIf { parentField == null } ?: field.copy(override = true)
    }

    // patch type with new set of fields and update it in type container
    patchedTypeContainer[this] = type.copy(fields = mergedFields)

    // for any overridden field patch its type with the base types
    mergedFields.filter { field -> field.override }.forEach { field ->
      val baseTypes = inheritedFields.mapNotNull { inheritedField ->
        inheritedField.takeIf { it.name == field.name }?.type
      }
      if (baseTypes.isNotEmpty()) field.type.patch(baseTypes)
    }
  }

  private fun CodeGenerationAst.FieldType.patch(parentFieldTypes: List<CodeGenerationAst.FieldType>) {
    when (this) {
      is CodeGenerationAst.FieldType.Object -> {
        parentFieldTypes.forEach { parentFieldType ->
          if (parentFieldType is CodeGenerationAst.FieldType.Object) {
            typeRef.patch(interfaceType = parentFieldType.typeRef)
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

  private fun CodeGenerationAst.TypeRef.patch(interfaceType: CodeGenerationAst.TypeRef) {
    if (this == interfaceType) return

    val objectTypeToPatch = patchedTypeContainer[this] as CodeGenerationAst.ObjectType
    patchedTypeContainer[this] = objectTypeToPatch.copy(implements = objectTypeToPatch.implements + interfaceType)

    if (objectTypeToPatch.kind is CodeGenerationAst.ObjectType.Kind.Fragment) {
      objectTypeToPatch.kind.defaultImplementation.patch()
      objectTypeToPatch.kind.possibleImplementations.values.forEach { typeRef ->
        typeRef.patch()
      }
    }

    patch()
  }

  private fun CodeGenerationAst.FieldType.Array.patch(
      parentFieldType: CodeGenerationAst.FieldType.Array
  ) {
    when (rawType) {
      is CodeGenerationAst.FieldType.Object -> {
        if (parentFieldType.rawType is CodeGenerationAst.FieldType.Object) {
          rawType.typeRef.patch(
              interfaceType = parentFieldType.rawType.typeRef
          )
        }
      }
      is CodeGenerationAst.FieldType.Array -> rawType.patch(
          parentFieldType = parentFieldType.rawType as CodeGenerationAst.FieldType.Array
      )
    }
  }

  private fun Set<CodeGenerationAst.TypeRef>.collectFields(): List<CodeGenerationAst.Field> {
    return flatMap { type ->
      (patchedTypeContainer[type] ?: fragmentTypeContainer[type])?.fields ?: emptyList()
    }
  }
}
