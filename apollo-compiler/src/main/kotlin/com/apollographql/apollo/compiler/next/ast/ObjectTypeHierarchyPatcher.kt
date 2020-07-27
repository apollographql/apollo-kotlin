package com.apollographql.apollo.compiler.next.ast

internal fun ObjectTypeContainer.patchTypeHierarchy(
    rootType: CodeGenerationAst.TypeRef,
    fragmentTypes: List<CodeGenerationAst.FragmentType>
): ObjectTypeContainer {
  val fragmentTypeContainer = fragmentTypes.fold(emptyMap<CodeGenerationAst.TypeRef, CodeGenerationAst.ObjectType>()) { acc, fragmentType ->
    acc + fragmentType.nestedTypes
  }
  return TypeHierarchyPatcher(this + fragmentTypeContainer)
      .patch(rootType)
      .minus(fragmentTypeContainer.keys)
}

private class TypeHierarchyPatcher(typeContainer: ObjectTypeContainer) {
  private val patchedTypeContainer = typeContainer.toMutableMap()

  fun patch(rootType: CodeGenerationAst.TypeRef): Map<CodeGenerationAst.TypeRef, CodeGenerationAst.ObjectType> {
    rootType.patch()
    return patchedTypeContainer
  }

  private fun CodeGenerationAst.TypeRef.patch() {
    val type = requireNotNull(patchedTypeContainer[this]) {
      "Can't resolve type, unknown type reference `$this`"
    }

    type.implements.forEach { parentType -> parentType.patch() }

    val parentTypeFields = type.implements
        .flatMap { parentType -> (patchedTypeContainer[parentType] as CodeGenerationAst.ObjectType).fields }

    val patchedFields = type.fields.map { field ->
      if (field.type.nullable) {
        val nonNullableParentField = parentTypeFields.find { parentField ->
          parentField.name == field.name && !parentField.type.nullable
        }
        check(nonNullableParentField == null) {
          "Nullable field `${field.responseName}` of type `${field.type}` defined on object `${name}` is not a subtype of non nullable " +
              "overridden field. Please use field alias instead."
        }
      }

      if (field.override) {
        field
      } else {
        field.copy(override = parentTypeFields.find { parentField -> parentField.name == field.name } != null)
      }
    }

    patchedTypeContainer[this] = type.copy(fields = patchedFields)

    patchedFields.forEach { field ->
      val parentFieldTypes = parentTypeFields.mapNotNull { parentField ->
        parentField.takeIf { it.name == field.name }?.type
      }
      when {
        parentFieldTypes.isEmpty() -> field.type.patch()
        else -> field.type.patch(parentFieldTypes)
      }
    }
  }

  private fun CodeGenerationAst.FieldType.patch() {
    when (this) {
      is CodeGenerationAst.FieldType.Object -> {
        typeRef.patch()
      }

      is CodeGenerationAst.FieldType.Array -> {
        rawType.patch()
      }

      is CodeGenerationAst.FieldType.Fragment -> {
        defaultType.patch()
        possibleTypes.values.forEach { type -> type.patch() }
      }
    }
  }

  private fun CodeGenerationAst.FieldType.patch(parentFieldTypes: List<CodeGenerationAst.FieldType>) {
    when (this) {
      is CodeGenerationAst.FieldType.Object -> {
        parentFieldTypes.forEach { parentFieldType ->
          if (parentFieldType is CodeGenerationAst.FieldType.Object) {
            patch(parentFieldType)
          } else if (parentFieldType is CodeGenerationAst.FieldType.Fragment) {
            patch(parentFieldType)
          }
        }
      }

      is CodeGenerationAst.FieldType.Array -> {
        parentFieldTypes.forEach { parentFieldType ->
          patch(parentFieldType as CodeGenerationAst.FieldType.Array)
        }
      }

      is CodeGenerationAst.FieldType.Fragment -> {
        parentFieldTypes.forEach { parentFieldType ->
          if (parentFieldType is CodeGenerationAst.FieldType.Object) {
            patch(parentFieldType)
          } else if (parentFieldType is CodeGenerationAst.FieldType.Fragment) {
            patch(parentFieldType)
          }
        }
      }
    }
  }

  private fun CodeGenerationAst.FieldType.Object.patch(
      parentFieldType: CodeGenerationAst.FieldType.Object
  ) {
    patchedTypeContainer[typeRef] = with(patchedTypeContainer[typeRef] as CodeGenerationAst.ObjectType) {
      copy(implements = implements + parentFieldType.typeRef)
    }
    typeRef.patch()
  }

  private fun CodeGenerationAst.FieldType.Object.patch(
      parentFieldType: CodeGenerationAst.FieldType.Fragment
  ) {
    patchedTypeContainer[typeRef] = with(patchedTypeContainer[typeRef] as CodeGenerationAst.ObjectType) {
      copy(implements = implements + parentFieldType.rawType)
    }
    typeRef.patch()
  }

  private fun CodeGenerationAst.FieldType.Fragment.patch(
      parentFieldType: CodeGenerationAst.FieldType.Object
  ) {
    patchedTypeContainer[defaultType] = with(patchedTypeContainer[defaultType] as CodeGenerationAst.ObjectType) {
      copy(implements = implements + parentFieldType.typeRef)
    }
    defaultType.patch()

    possibleTypes.values.forEach { typeRef ->
      patchedTypeContainer[typeRef] = with(patchedTypeContainer[typeRef] as CodeGenerationAst.ObjectType) {
        copy(implements = implements + parentFieldType.typeRef)
      }
      typeRef.patch()
    }
  }

  private fun CodeGenerationAst.FieldType.Fragment.patch(
      parentFieldType: CodeGenerationAst.FieldType.Fragment
  ) {
    patchedTypeContainer[defaultType] = with(patchedTypeContainer[defaultType] as CodeGenerationAst.ObjectType) {
      copy(implements = implements + parentFieldType.rawType)
    }
    defaultType.patch()

    possibleTypes.values.forEach { typeRef ->
      patchedTypeContainer[typeRef] = with(patchedTypeContainer[typeRef] as CodeGenerationAst.ObjectType) {
        copy(implements = implements + parentFieldType.rawType)
      }
      typeRef.patch()
    }
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
        } else if (parentFieldType.rawType is CodeGenerationAst.FieldType.Fragment) {
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
