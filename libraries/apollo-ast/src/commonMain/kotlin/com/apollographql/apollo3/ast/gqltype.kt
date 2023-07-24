package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloExperimental

@Deprecated("Use rawType instead", ReplaceWith("rawType()"))
fun GQLType.leafType(): GQLNamedType = rawType()

/**
 * Returns the raw type. The raw type is the GQLNamedType without any list/nonnull wrapper types
 */
fun GQLType.rawType(): GQLNamedType = when (this) {
  is GQLNonNullType -> type.rawType()
  is GQLListType -> type.rawType()
  is GQLNamedType -> this
}

fun GQLType.pretty(): String = when (this) {
  is GQLNonNullType -> "${type.pretty()}!"
  is GQLListType -> "[${type.pretty()}]"
  is GQLNamedType -> this.name
}

internal fun isVariableUsageAllowed(variableDefinition: GQLVariableDefinition, usage: VariableUsage): Boolean {
  val variableType = variableDefinition.type
  val locationType = usage.locationType

  if (locationType is GQLNonNullType && variableType !is GQLNonNullType) {
    if (variableDefinition.defaultValue == null && !usage.hasLocationDefaultValue) {
      return false
    }

    return areTypesCompatible(variableType, locationType.type)
  }
  return areTypesCompatible(variableType, locationType)
}

internal fun areTypesCompatible(variableType: GQLType, locationType: GQLType): Boolean {
  return if(locationType is GQLNonNullType) {
    if (variableType !is GQLNonNullType) {
      false
    } else {
      areTypesCompatible(variableType.type, locationType.type)
    }
  } else if (variableType is GQLNonNullType) {
    areTypesCompatible(variableType.type, locationType)
  } else if (locationType is GQLListType){
    if (variableType !is GQLListType) {
      false
    } else {
      areTypesCompatible(variableType.type, locationType.type)
    }
  } else if (variableType is GQLListType) {
    false
  } else {
    /**
     * At this point, both variableType and locationType must be input objects
     * If this is not the case, this means variables validation has failed
     */
    check(variableType is GQLNamedType)
    check(locationType is GQLNamedType)
    variableType.name == locationType.name
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

private fun GQLNullability?.selfNullability(): GQLNullability? {
  return when (this) {
    is GQLListNullability -> this.selfNullability
    else -> this
  }
}

private fun GQLType.withListNullability(nullability: GQLNullability?): GQLType {
  if (this is GQLListType && nullability is GQLListNullability) {
    return copy(type = type.withNullability(nullability.itemNullability))
  } else if (this is GQLListType && nullability !is GQLListNullability) {
    return this
  } else if (this !is GQLListType && nullability is GQLListNullability) {
    return this
  } else if (this !is GQLListType && nullability !is GQLListNullability) {
    return this
  } else {
    error("")
  }
}

@ApolloExperimental
fun GQLType.withNullability(nullability: GQLNullability?): GQLType {
  val selfNullability = nullability.selfNullability()

  if (this is GQLNonNullType && selfNullability == null) {
    return this.copy(type = type.withListNullability(nullability))
  } else if (this is GQLNonNullType && selfNullability is GQLNonNullDesignator) {
    return this.copy(type = type.withListNullability(nullability))
  } else if (this is GQLNonNullType && selfNullability is GQLNullDesignator) {
    return this.type.withListNullability(nullability)
  } else if (this !is GQLNonNullType && selfNullability == null) {
    return this.withListNullability(nullability)
  } else if (this !is GQLNonNullType && selfNullability is GQLNonNullDesignator) {
    return GQLNonNullType(type = this.withListNullability(nullability))
  } else if (this !is GQLNonNullType && selfNullability is GQLNullDesignator) {
    return this.withListNullability(nullability)
  } else {
    error("")
  }
}