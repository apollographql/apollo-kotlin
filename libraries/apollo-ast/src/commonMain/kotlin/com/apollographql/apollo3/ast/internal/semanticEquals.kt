package com.apollographql.apollo3.ast.internal

import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLDirective
import com.apollographql.apollo3.ast.GQLDirectiveDefinition
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLEnumValue
import com.apollographql.apollo3.ast.GQLFloatValue
import com.apollographql.apollo3.ast.GQLIntValue
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLListValue
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLNullValue
import com.apollographql.apollo3.ast.GQLObjectValue
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLValue
import com.apollographql.apollo3.ast.GQLVariableValue
import com.apollographql.apollo3.ast.toUtf8

internal fun GQLDirectiveDefinition.semanticEquals(other: GQLDirectiveDefinition): Boolean {
  if (name != other.name) {
    return false
  }

  if (locations != other.locations) {
    return false
  }

  if (repeatable != other.repeatable) {
    return false
  }

  if (arguments.size != other.arguments.size) {
    return false
  }

  arguments.forEach { argument ->
    val otherArgument = other.arguments.firstOrNull { it.name == argument.name }
    if (otherArgument == null) {
      return false
    }

    if (!argument.type.semanticEquals(otherArgument.type)) {
      return false
    }

    if (argument.defaultValue != null && otherArgument.defaultValue != null) {
      if (!argument.defaultValue.semanticEquals(otherArgument.defaultValue)) {
        return false
      }
    } else if (argument.defaultValue != null || otherArgument.defaultValue != null) {
      return false
    }
  }

  return true
}

internal fun GQLType.semanticEquals(other: GQLType): Boolean {
  return when (this) {
    is GQLNonNullType -> {
      other is GQLNonNullType && type.semanticEquals(other.type)
    }

    is GQLListType -> {
      other is GQLListType && type.semanticEquals(other.type)
    }

    is GQLNamedType -> {
      other is GQLNamedType && name == other.name
    }
  }
}

internal fun GQLValue.semanticEquals(other: GQLValue): Boolean {
  return when (this) {
    is GQLNullValue -> {
      other is GQLNullValue
    }

    is GQLListValue -> {
      other is GQLListValue && values.size == other.values.size && values.zip(other.values).all { (a, b) -> a.semanticEquals(b) }
    }

    is GQLObjectValue -> {
      other is GQLObjectValue && fields.size == other.fields.size && fields.sortedBy { it.name }.zip(other.fields.sortedBy { it.name }).all { (a, b) ->
        a.name == b.name && a.value.semanticEquals(b.value)
      }
    }

    is GQLStringValue -> {
      other is GQLStringValue && value == other.value
    }

    is GQLBooleanValue -> {
      other is GQLBooleanValue && value == other.value
    }

    is GQLIntValue -> {
      other is GQLIntValue && value == other.value
    }

    is GQLFloatValue -> {
      other is GQLFloatValue && value == other.value
    }

    is GQLEnumValue -> {
      other is GQLEnumValue && value == other.value
    }

    is GQLVariableValue -> {
      other is GQLVariableValue && name == other.name
    }
  }
}


internal fun GQLEnumTypeDefinition.semanticEquals(definition: GQLEnumTypeDefinition): Boolean {
  if (name != definition.name) {
    return false
  }

  if (directives.size != definition.directives.size) {
    return false
  }

  directives.sortedBy { it.name }.zip(definition.directives.sortedBy { it.name }).forEach { (a, b) ->
    if (!a.semanticEquals(b)) {
      return false
    }
  }

  if (enumValues.size != definition.enumValues.size) {
    return false
  }

  enumValues.forEach { value ->
    val otherValue = definition.enumValues.firstOrNull { it.name == value.name }
    if (otherValue == null) {
      return false
    }

    if (value.directives.size != otherValue.directives.size) {
      return false
    }

    if (value.directives.sortedBy { it.name }.zip(otherValue.directives.sortedBy { it.name }).any { (a, b) -> !a.semanticEquals(b) }) {
      return false
    }
  }

  return true
}

private fun GQLDirective.semanticEquals(other: GQLDirective): Boolean {
  if (name != other.name) {
    return false
  }

  if (arguments.size != other.arguments.size) {
    return false
  }

  arguments.forEach { argument ->
    val otherArgument = other.arguments.firstOrNull { it.name == argument.name }
    if (otherArgument == null) {
      return false
    }

    if (!argument.value.semanticEquals(otherArgument.value)) {
      return false
    }
  }

  return true
}


internal fun GQLDirectiveDefinition.toSemanticSdl(): String {
  return copy(description = null, arguments = arguments.map { it.copy(description = null) }).toUtf8().trim()
}

internal fun GQLEnumTypeDefinition.toSemanticSdl(): String {
  return copy(description = null, enumValues = enumValues.map { it.copy(description = null) }).toUtf8().replace(Regex("[\\n ]+"), " ").trim()
}
