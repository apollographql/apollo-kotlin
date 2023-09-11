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
  return if (locationType is GQLNonNullType) {
    if (variableType !is GQLNonNullType) {
      false
    } else {
      areTypesCompatible(variableType.type, locationType.type)
    }
  } else if (variableType is GQLNonNullType) {
    areTypesCompatible(variableType.type, locationType)
  } else if (locationType is GQLListType) {
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

private fun GQLType.withItemNullability(itemNullability: GQLNullability?, validation: NullabilityValidation): GQLType {
  if (itemNullability == null) {
    return this
  }

  if (this !is GQLListType) {
    when (validation) {
      is NullabilityValidationThrow -> {
        error("Cannot apply nullability, the nullability list dimension exceeds the one of the field type.")
      }

      is NullabilityValidationIgnore -> {
        return this
      }

      is NullabilityValidationRegister -> {
        validation.issues.add(
            OtherValidationIssue(
                "Cannot apply nullability on '${validation.fieldName}', the nullability list dimension exceeds the one of the field type.",
                itemNullability.sourceLocation,
            )
        )
        return this
      }
    }
  }

  return this.copy(type = type.withNullability(itemNullability, validation))
}

@ApolloExperimental
fun GQLType.withNullability(nullability: GQLNullability?): GQLType {
  return withNullability(nullability, NullabilityValidationThrow)
}

internal sealed interface NullabilityValidation

internal object NullabilityValidationIgnore: NullabilityValidation
internal object NullabilityValidationThrow: NullabilityValidation
internal class NullabilityValidationRegister(val issues: MutableList<Issue>, val fieldName: String): NullabilityValidation

internal fun GQLType.withNullability(nullability: GQLNullability?, validation: NullabilityValidation): GQLType {
  val selfNullability: GQLNullability?
  val itemNullability: GQLNullability?

  when (nullability) {
    is GQLListNullability -> {
      selfNullability = nullability.selfNullability
      itemNullability = nullability.itemNullability
    }

    else -> {
      selfNullability = nullability
      itemNullability = null
    }
  }
  return if (this is GQLNonNullType && selfNullability == null) {
    this.copy(type = type.withItemNullability(itemNullability, validation))
  } else if (this is GQLNonNullType && selfNullability is GQLNonNullDesignator) {
    this.copy(type = type.withItemNullability(itemNullability, validation))
  } else if (this is GQLNonNullType && selfNullability is GQLNullDesignator) {
    this.type.withItemNullability(itemNullability, validation)
  } else if (this !is GQLNonNullType && selfNullability == null) {
    this.withItemNullability(itemNullability, validation)
  } else if (this !is GQLNonNullType && selfNullability is GQLNonNullDesignator) {
    GQLNonNullType(type = this.withItemNullability(itemNullability, validation))
  } else if (this !is GQLNonNullType && selfNullability is GQLNullDesignator) {
    this.withItemNullability(itemNullability, validation)
  } else {
    error("")
  }
}