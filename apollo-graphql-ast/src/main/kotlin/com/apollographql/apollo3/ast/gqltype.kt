package com.apollographql.apollo3.ast


fun GQLType.leafType(): GQLNamedType = when (this) {
  is GQLNonNullType -> type.leafType()
  is GQLListType -> type.leafType()
  is GQLNamedType -> this
}

fun GQLType.pretty(): String = when (this) {
  is GQLNonNullType -> "${type.pretty()}!"
  is GQLListType -> "[${type.pretty()}]"
  is GQLNamedType -> this.name
}

internal fun GQLType.canInputValueBeAssignedTo(target: GQLType): Boolean {
  return when (this) {
    is GQLNonNullType -> when (target) {
      // non-null can always be assigned to their nullable equivalents
      is GQLNonNullType -> type.canInputValueBeAssignedTo(target.type)
      is GQLListType -> type.canInputValueBeAssignedTo(target)
      is GQLNamedType -> type.canInputValueBeAssignedTo(target)
    }
    is GQLListType -> when (target) {
      // lists are covariant. [CatInput!] can be passed where [CatInput] is expected
      is GQLListType -> type.canInputValueBeAssignedTo(target.type)
      is GQLNonNullType -> false
      is GQLNamedType -> false
    }
    is GQLNamedType -> when (target) {
      is GQLNonNullType -> false
      is GQLListType -> false
      is GQLNamedType -> {
        /**
         * At this point, both this and target must be input objects
         * If this is not the case, this means variables validation has failed
         */
        return name == target.name
      }
    }
  }
}

internal fun GQLType.isInputType(typeDefinitions: Map<String, GQLTypeDefinition>): Boolean = when (this) {
  is GQLNonNullType -> this.type.isInputType(typeDefinitions)
  is GQLListType -> this.type.isInputType(typeDefinitions)
  is GQLNamedType -> typeDefinitions[name].let {
    it is GQLInputObjectTypeDefinition
        || it is GQLScalarTypeDefinition
        || it is GQLEnumTypeDefinition
  }
}

internal fun GQLType.isOutputType(typeDefinitions: Map<String, GQLTypeDefinition>): Boolean = when (this) {
  is GQLNonNullType -> this.type.isInputType(typeDefinitions)
  is GQLListType -> this.type.isInputType(typeDefinitions)
  is GQLNamedType -> typeDefinitions[name].let {
    it is GQLObjectTypeDefinition
        || it is GQLUnionTypeDefinition
        || it is GQLInterfaceTypeDefinition
        || it is GQLScalarTypeDefinition
        || it is GQLEnumTypeDefinition
  }
}
