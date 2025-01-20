package com.apollographql.apollo.execution.internal

import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLEnumValue
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLListType
import com.apollographql.apollo.ast.GQLListValue
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLNullValue
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLObjectValue
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.GQLValue
import com.apollographql.apollo.ast.GQLVariableValue
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.definitionFromScope
import com.apollographql.apollo.execution.Coercing
import com.apollographql.apollo.execution.InternalValue
import com.apollographql.apollo.execution.scalarCoercingParseLiteral
import com.apollographql.apollo.execution.toInternalValue

internal fun coerceArgumentValues(
  schema: Schema,
  typename: String,
  field: GQLField,
  coercings: Map<String, Coercing<*>>,
  coercedVariables: Map<String, InternalValue>,
): Map<String, InternalValue> {
  val coercedValues = mutableMapOf<String, InternalValue>()
  val argumentValues = field.arguments.associate { it.name to it.value }
  val argumentDefinitions = field.definitionFromScope(schema, typename)!!.arguments

  argumentDefinitions.forEach { argumentDefinition ->
    val argumentName = argumentDefinition.name
    val argumentType = argumentDefinition.type
    val defaultValue = argumentDefinition.defaultValue
    var hasValue = argumentValues.containsKey(argumentName)
    val argumentValue = argumentValues.get(argumentName)
    var value: Any?
    if (argumentValue is GQLVariableValue) {
      val variableName = argumentValue.name
      hasValue = coercedVariables.containsKey(variableName)
      value = coercedVariables.get(variableName)
    } else {
      value = argumentValue
    }
    if (!hasValue && defaultValue != null) {
      hasValue = true
      value = defaultValue
    } else {
      if (argumentType is GQLNonNullType) {
        if (!hasValue) {
          error("No value passed for required argument: '$argumentName'")
        }
        if (value == null) {
          error("'null' found in non-null position for argument: '$argumentName'")
        }
      }
    }

    if (hasValue) {
      val value2 = if (value == null) {
        null
      } else if (argumentValue is GQLVariableValue) {
        value
      } else if (value is GQLValue) {
        coerceLiteralToInternal(schema, value, argumentType, coercings, coercedVariables)
      } else {
        error("Cannot coerce '$value'")
      }
      coercedValues.put(argumentName, value2)
    }
  }

  return coercedValues
}

/**
 *
 */
internal fun coerceLiteralToInternal(schema: Schema, value: GQLValue, type: GQLType, coercings: Map<String, Coercing<*>>, coercedVariables: Map<String, InternalValue>): InternalValue {
  if (value is GQLNullValue) {
    check(type !is GQLNonNullType) {
      error("'null' found in non-null position")
    }
    return null
  }
  if (value is GQLVariableValue) {
    /**
     * The absence of a variable MUST be checked at the callsite
     *
     * This is done for:
     * - top level argument values
     * - input objects
     * - lists
     */
    return coercedVariables.get(value.name)
  }

  return when (type) {
    is GQLNonNullType -> {
      coerceLiteralToInternal(schema, value, type.type, coercings, coercedVariables)
    }

    is GQLListType -> {
      if (value is GQLListValue) {
        value.values.map {
          if (it is GQLVariableValue) {
            if (coercedVariables.containsKey(it.name)) {
              coercedVariables.get(it.name)
            } else {
              // In lists, absent variables are coerced to null
              null
            }
          } else {
            coerceLiteralToInternal(schema, it, type.type, coercings, coercedVariables)
          }
        }
      } else {
        if (value is GQLVariableValue) {
          if (coercedVariables.containsKey(value.name)) {
            coercedVariables.get(value.name)
          } else {
            null
          }
        } else {
          // Single items are mapped to a list of 1
          listOf(coerceLiteralToInternal(schema, value, type.type, coercings, coercedVariables))
        }
      }
    }

    is GQLNamedType -> {
      val definition = schema.typeDefinition(type.name)
      when (definition) {
        is GQLEnumTypeDefinition -> {
          coerceEnumLiteralToInternal(value, coercings, definition)
        }

        is GQLInputObjectTypeDefinition -> {
          coerceInputObject(schema, definition, value, coercings, coercedVariables)
        }

        is GQLInterfaceTypeDefinition,
        is GQLObjectTypeDefinition,
        is GQLUnionTypeDefinition,
        -> {
          error("Output type '${definition.name}' cannot be used in input position")
        }

        is GQLScalarTypeDefinition -> {
          scalarCoercingParseLiteral(value, coercings, definition.name)
        }
      }
    }
  }
}

internal fun coerceEnumLiteralToInternal(value: GQLValue, coercings: Map<String, Coercing<*>>, definition: GQLEnumTypeDefinition): InternalValue {
  check(value is GQLEnumValue) {
    error("Don't know how to coerce '$value' to a '${definition.name}' enum value")
  }

  val coercing = coercings.get(definition.name)

  return if (coercing == null) {
    check(definition.enumValues.any { it.name == value.value }) {
      val possibleValues = definition.enumValues.map { it.name }.toSet()
      "'$value' cannot be coerced to a '${definition.name}' enum value. Possible values are: '$possibleValues'"
    }
    value.value
  } else {
    coercing.parseLiteral(value)
  }
}

private fun coerceInputObject(schema: Schema, definition: GQLInputObjectTypeDefinition, literalValue: GQLValue, coercings: Map<String, Coercing<*>>, coercedVariables: Map<String, InternalValue>): InternalValue {
  if (literalValue !is GQLObjectValue) {
    error("Don't know how to coerce '$literalValue' to a '${definition.name}' input object")
  }

  val fields = literalValue.fields.associate { it.name to it.value }

  val map = definition.inputFields.mapNotNull { inputValueDefinition ->
    val inputFieldType = inputValueDefinition.type

    if (!fields.containsKey(inputValueDefinition.name)) {
      if (inputValueDefinition.defaultValue != null) {
        inputValueDefinition.name to inputValueDefinition.defaultValue!!.toInternalValue()
      } else {
        if (inputFieldType is GQLNonNullType) {
          error("Missing input field '${inputValueDefinition.name}")
        }
        // Skip this field
        null
      }
    } else {
      val inputFieldValue = fields.get(inputValueDefinition.name)!!
      inputValueDefinition.name to coerceLiteralToInternal(schema, inputFieldValue, inputValueDefinition.type, coercings, coercedVariables)
    }
  }.toMap()

  val coercing = coercings.get(definition.name)
  return if (coercing != null) {
    coercing.deserialize(map)
  } else {
    map
  }
}
