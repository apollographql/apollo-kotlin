package com.apollographql.apollo3.ast.internal

import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLEnumValue
import com.apollographql.apollo3.ast.GQLFloatValue
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLIntValue
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLListValue
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLNullValue
import com.apollographql.apollo3.ast.GQLObjectField
import com.apollographql.apollo3.ast.GQLObjectValue
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLValue
import com.apollographql.apollo3.ast.GQLVariableValue
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.VariableUsage
import com.apollographql.apollo3.ast.findDeprecationReason
import com.apollographql.apollo3.ast.isDeprecated
import com.apollographql.apollo3.ast.pretty
import com.apollographql.apollo3.ast.toUtf8

internal fun ValidationScope.validateAndCoerceValueInConstContext(
    value: GQLValue,
    expectedType: GQLType,
    hasLocationDefaultValue: Boolean,
): GQLValue {
  return validateAndCoerceValue(value, expectedType, hasLocationDefaultValue) {
    issues.add(it.constContextError())
  }
}

internal fun VariableUsage.constContextError(): Issue = Issue.ValidationError(
    message = "Variable '${variable.name}' used in non-variable context",
    sourceLocation = variable.sourceLocation
)

internal fun ValidationScope.validateAndCoerceValue(
    value: GQLValue,
    expectedType: GQLType,
    hasLocationDefaultValue: Boolean,
    registerVariableUsage: (VariableUsage) -> Unit,
): GQLValue {
  if (value is GQLVariableValue) {
    registerVariableUsage(
        VariableUsage(
            value,
            expectedType,
            hasLocationDefaultValue
        )
    )

    return value
  } else if (value is GQLNullValue) {
    if (expectedType is GQLNonNullType) {
      registerIssue(value, expectedType)
      return value
    }

    // null is always valid in a nullable position
    return value
  }

  when (expectedType) {
    is GQLNonNullType -> {
      return validateAndCoerceValue(value, expectedType.type, hasLocationDefaultValue, registerVariableUsage)
    }
    is GQLListType -> {
      val coercedValue = if (value !is GQLListValue) {
        /**
         * http://spec.graphql.org/draft/#sec-List.Input-Coercion
         *
         * Single values are coerced to lists
         */
        GQLListValue(sourceLocation = value.sourceLocation, listOf(value))
      } else {
        value
      }
      return GQLListValue(
          values = coercedValue.values.map {
            /**
             * When using a GQLListValue like `[$variable, 1, 3]`, it's not possible to have a default location value
             */
            validateAndCoerceValue(it, expectedType.type, false, registerVariableUsage)
          }
      )
    }
    is GQLNamedType -> {
      when (val expectedTypeDefinition = typeDefinitions[expectedType.name]) {
        is GQLInputObjectTypeDefinition -> {
          return validateAndCoerceInputObject(value, expectedTypeDefinition, registerVariableUsage)
        }
        is GQLScalarTypeDefinition -> {
          if (!expectedTypeDefinition.isBuiltIn()) {
            // custom scalar types are passed through
            return value
          }
          return validateAndCoerceScalar(value, expectedType)
        }
        is GQLEnumTypeDefinition -> {
          return validateAndCoerceEnum(value, expectedTypeDefinition)
        }
        else -> {
          registerIssue("Value cannot be of non-input type ${expectedType.pretty()}", value.sourceLocation)
          return value
        }
      }
    }
  }
}

private fun ValidationScope.registerIssue(value: GQLValue, expectedType: GQLType) {
  registerIssue(message = "Value `${value.toUtf8()}` cannot be used in position expecting `${expectedType.pretty()}`", sourceLocation = value.sourceLocation)
}

private fun ValidationScope.validateAndCoerceInputObject(
    value: GQLValue,
    expectedTypeDefinition: GQLInputObjectTypeDefinition,
    registerVariableUsage: (VariableUsage) -> Unit,
): GQLValue {
  val expectedType = GQLNamedType(name = expectedTypeDefinition.name)
  if (value !is GQLObjectValue) {
    registerIssue(value, expectedType)
    return value
  }

  expectedTypeDefinition.inputFields.forEach { inputValueDefinition ->
    // 3.10 All required input fields must have a value
    if (inputValueDefinition.type is GQLNonNullType
        && inputValueDefinition.defaultValue == null
        && value.fields.firstOrNull { it.name == inputValueDefinition.name } == null
    ) {
      registerIssue(message = "No value passed for required inputField `${inputValueDefinition.name}`", sourceLocation = value.sourceLocation)
    }
    if (inputValueDefinition.directives.findDeprecationReason() != null) {
      issues.add(
          Issue.DeprecatedUsage(
              message = "Use of deprecated input field `${inputValueDefinition.name}`",
              sourceLocation = value.sourceLocation
          )
      )
    }

  }

  return GQLObjectValue(fields = value.fields.mapNotNull { field ->
    val inputField = expectedTypeDefinition.inputFields.firstOrNull { it.name == field.name }
    if (inputField == null) {
      // 3.10 Input values coercion: extra values are errors
      registerIssue(message = "Field ${field.name} is not defined by ${expectedType.pretty()}", sourceLocation = field.sourceLocation)
      return@mapNotNull null
    }
    GQLObjectField(
        name = field.name,
        value = validateAndCoerceValue(field.value, inputField.type, inputField.defaultValue != null, registerVariableUsage)
    )
  })
}

private fun ValidationScope.validateAndCoerceEnum(value: GQLValue, enumTypeDefinition: GQLEnumTypeDefinition): GQLValue {
  val expectedType = GQLNamedType(name = enumTypeDefinition.name)
  if (value !is GQLEnumValue) {
    registerIssue(value, expectedType)
    return value
  }

  val enumValue = enumTypeDefinition.enumValues.firstOrNull { it.name == value.value }
  if (enumValue == null) {
    registerIssue(
        message = "Cannot find enum value `${value.value}` of type `${enumTypeDefinition.name}`",
        sourceLocation = value.sourceLocation
    )
  } else if (enumValue.isDeprecated()) {
    issues.add(Issue.DeprecatedUsage(
        message = "Use of deprecated enum value `${value.value}` of type `${enumTypeDefinition.name}`",
        sourceLocation = value.sourceLocation
    ))
  }
  return value
}

private fun ValidationScope.validateAndCoerceScalar(value: GQLValue, expectedType: GQLNamedType): GQLValue {
  return when (expectedType.name) {
    "Int" -> {
      if (value !is GQLIntValue) {
        registerIssue(value, expectedType)
      }
      value
    }
    "Float" -> {
      when (value) {
        is GQLFloatValue -> value
        // Int get coerced to floats
        is GQLIntValue -> GQLFloatValue(value = value.value.toDouble())
        else -> {
          registerIssue(value, expectedType)
          value
        }
      }
    }
    "String" -> {
      if (value !is GQLStringValue) {
        registerIssue(value, expectedType)
      }
      value
    }
    "Boolean" -> {
      if (value !is GQLBooleanValue) {
        registerIssue(value, expectedType)
      }
      value
    }
    "ID" -> {
      // 3.5.5 ID can be either string or int
      if (value !is GQLStringValue && value !is GQLIntValue) {
        registerIssue(value, expectedType)
      }
      value
    }
    else -> {
      registerIssue(value, expectedType)
      value
    }
  }
}

