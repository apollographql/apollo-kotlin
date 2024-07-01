package com.apollographql.apollo.ast.internal

import com.apollographql.apollo.ast.GQLArgument
import com.apollographql.apollo.ast.GQLBooleanValue
import com.apollographql.apollo.ast.GQLDirective
import com.apollographql.apollo.ast.GQLDirectiveDefinition
import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLEnumValue
import com.apollographql.apollo.ast.GQLEnumValueDefinition
import com.apollographql.apollo.ast.GQLFloatValue
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLInputValueDefinition
import com.apollographql.apollo.ast.GQLIntValue
import com.apollographql.apollo.ast.GQLListType
import com.apollographql.apollo.ast.GQLListValue
import com.apollographql.apollo.ast.GQLNamed
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNode
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLNullValue
import com.apollographql.apollo.ast.GQLObjectValue
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLVariableValue
import com.apollographql.apollo.ast.toUtf8

/**
 * Returns true if the two nodes are semantically equal, which ignores the source location and the description.
 * Note that not all cases are implemented - currently [GQLEnumTypeDefinition] and [GQLDirectiveDefinition] are fully supported, and
 * unsupported types will throw.
 */
internal fun GQLNode.semanticEquals(other: GQLNode?): Boolean {
  if (other == null) return false
  when (this) {
    is GQLDirectiveDefinition -> {
      if (other !is GQLDirectiveDefinition) {
        return false
      }

      if (locations != other.locations) {
        return false
      }

      if (repeatable != other.repeatable) {
        return false
      }
    }

    is GQLInputValueDefinition -> {
      if (other !is GQLInputValueDefinition) {
        return false
      }

      if (!type.semanticEquals(other.type)) {
        return false
      }

      if (defaultValue != null) {
        if (!defaultValue.semanticEquals(other.defaultValue)) {
          return false
        }
      } else if (other.defaultValue != null) {
        return false
      }
    }

    is GQLNonNullType -> {
      if (other !is GQLNonNullType) {
        return false
      }
    }

    is GQLListType -> {
      if (other !is GQLListType) {
        return false
      }
    }

    is GQLNamedType -> {
      if (other !is GQLNamedType) {
        return false
      }
    }

    is GQLNullValue -> {
      if (other !is GQLNullValue) {
        return false
      }
    }

    is GQLListValue -> {
      if (other !is GQLListValue) {
        return false
      }
    }

    is GQLObjectValue -> {
      if (other !is GQLObjectValue) {
        return false
      }
    }

    is GQLStringValue -> {
      if (other !is GQLStringValue) {
        return false
      }
      if (value != other.value) {
        return false
      }
    }

    is GQLBooleanValue -> {
      if (other !is GQLBooleanValue) {
        return false
      }
      if (value != other.value) {
        return false
      }
    }

    is GQLIntValue -> {
      if (other !is GQLIntValue) {
        return false
      }
      if (value != other.value) {
        return false
      }
    }

    is GQLFloatValue -> {
      if (other !is GQLFloatValue) {
        return false
      }
      if (value != other.value) {
        return false
      }
    }

    is GQLEnumValue -> {
      if (other !is GQLEnumValue) {
        return false
      }
      if (value != other.value) {
        return false
      }
    }

    is GQLVariableValue -> {
      if (other !is GQLVariableValue) {
        return false
      }
    }

    is GQLEnumTypeDefinition -> {
      if (other !is GQLEnumTypeDefinition) {
        return false
      }
    }

    is GQLDirective -> {
      if (other !is GQLDirective) {
        return false
      }
    }

    is GQLArgument -> {
      if (other !is GQLArgument) {
        return false
      }
    }

    is GQLEnumValueDefinition -> {
      if (other !is GQLEnumValueDefinition) {
        return false
      }
    }

    is GQLInputObjectTypeDefinition -> {
      if (other !is GQLInputObjectTypeDefinition) {
        return false
      }
    }

    is GQLScalarTypeDefinition -> {
      if (other !is GQLScalarTypeDefinition) {
        return false
      }
    }

    else -> {
      TODO("semanticEquals not supported for ${this::class.simpleName}")
    }
  }

  if (this is GQLNamed) {
    if (other !is GQLNamed) {
      return false
    }
    if (name != other.name) {
      return false
    }
  }

  if (children.size != other.children.size) {
    return false
  }
  for (i in children.indices) {
    if (!children[i].semanticEquals(other.children[i])) {
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
