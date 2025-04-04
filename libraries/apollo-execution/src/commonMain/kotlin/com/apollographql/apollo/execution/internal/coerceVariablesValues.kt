package com.apollographql.apollo.execution.internal

import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLListType
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.GQLVariableDefinition
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.execution.Coercing
import com.apollographql.apollo.execution.ExternalValue
import com.apollographql.apollo.execution.InternalValue
import com.apollographql.apollo.execution.scalarCoercingDeserialize

internal fun coerceVariableValues(
  schema: Schema,
  variableDefinitions: List<GQLVariableDefinition>,
  variables: Map<String, ExternalValue>,
  coercings: Map<String, Coercing<*>>,
): Map<String, InternalValue> {
  val coercedValues = mutableMapOf<String, InternalValue>()

  variableDefinitions.forEach { variableDefinition ->
    val hasValue = variables.containsKey(variableDefinition.name)
    if (hasValue) {
      val defaultValue = variableDefinition.defaultValue
      if (defaultValue != null) {
        coercedValues.put(variableDefinition.name, coerceInputLiteralToInternal(schema, defaultValue, variableDefinition.type, coercings, null))
        return@forEach
      } else if (variableDefinition.type !is GQLNonNullType) {
        // No value and nullable type, skip it
        return@forEach
      } else {
        error("No variable found for variable of non-null type '${variableDefinition.name}'")
      }
    }

    val value = variables.get(variableDefinition.name)
    coercedValues.put(variableDefinition.name, coerceExternalToInternal(schema, value, variableDefinition.type, coercings))
  }

  return coercedValues
}

/**
 *
 */
private fun coerceExternalToInternal(schema: Schema, value: ExternalValue, type: GQLType, coercings: Map<String, Coercing<*>>): InternalValue {
  if (value == null) {
    check(type !is GQLNonNullType) {
      error("'null' found in non-null position")
    }

    return null
  }

  return when (type) {
    is GQLNonNullType -> {
      coerceExternalToInternal(schema, value, type.type, coercings)
    }

    is GQLListType -> {
      if (value is List<*>) {
        value.map { coerceExternalToInternal(schema, it, type.type, coercings) }
      } else {
        // Single items are mapped to a list of 1
        listOf(coerceExternalToInternal(schema, value, type.type, coercings))
      }
    }

    is GQLNamedType -> {
      val definition = schema.typeDefinition(type.name)
      when (definition) {
        is GQLEnumTypeDefinition -> {
          coerceEnumExternalToInternal(value = value, coercings = coercings, definition = definition)
        }

        is GQLInputObjectTypeDefinition -> {
          coerceInputObject(schema, definition, value, coercings)
        }

        is GQLInterfaceTypeDefinition,
        is GQLObjectTypeDefinition,
        is GQLUnionTypeDefinition,
        -> {
          error("Output type '${definition.name}' cannot be used in input position")
        }

        is GQLScalarTypeDefinition -> {
          scalarCoercingDeserialize(value, coercings, definition.name)
        }
      }
    }
  }
}

internal fun coerceEnumExternalToInternal(value: ExternalValue, coercings: Map<String, Coercing<*>>, definition: GQLEnumTypeDefinition): InternalValue {
  check(value is String) {
    error("Don't know how to coerce '$value' to a '${definition.name}' enum value")
  }

  val coercing = coercings.get(definition.name)

  return if (coercing == null) {
    check(definition.enumValues.any { it.name == value }) {
      val possibleValues = definition.enumValues.map { it.name }.toSet()
      "'$value' cannot be coerced to a '${definition.name}' enum value. Possible values are: '$possibleValues'"
    }
    value
  } else {
    coercing.deserialize(value)
  }
}


private fun coerceInputObject(schema: Schema, definition: GQLInputObjectTypeDefinition, externalValue: ExternalValue, coercings: Map<String, Coercing<*>>): InternalValue {
  if (externalValue !is Map<*, *>) {
    error("Don't know how to coerce '$externalValue' to a '${definition.name}' input object")
  }
  val map = definition.inputFields.mapNotNull { inputValueDefinition ->
    val inputFieldType = inputValueDefinition.type
    if (!externalValue.containsKey(inputValueDefinition.name)) {
      if (inputValueDefinition.defaultValue != null) {
        inputValueDefinition.name to coerceInputLiteralToInternal(schema, inputValueDefinition.defaultValue!!, inputFieldType, coercings, null)
      } else {
        if (inputFieldType is GQLNonNullType) {
          error("Missing input field '${inputValueDefinition.name}")
        }
        // Skip this field
        null
      }
    } else {
      val inputFieldValue = externalValue.get(inputValueDefinition.name)
      inputValueDefinition.name to coerceExternalToInternal(schema, inputFieldValue, inputFieldType, coercings)
    }
  }.toMap()

  val coercing = coercings.get(definition.name)
  return if (coercing != null) {
    coercing.deserialize(map)
  } else {
    map
  }
}
