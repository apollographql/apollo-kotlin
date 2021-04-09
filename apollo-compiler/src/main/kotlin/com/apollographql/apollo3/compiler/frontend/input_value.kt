package com.apollographql.apollo3.compiler.frontend

internal class InputValueScope(val schema: Schema) {
  private val issues = mutableListOf<Issue>()
  private val variableReferences = mutableListOf<VariableReference>()

  class VariableReference(
      val variable: GQLVariableValue,
      val expectedType: GQLType
  )

  class Result(
      val coercedValue: GQLValue,
      val variableReferences: List<VariableReference>,
      val issues: List<Issue>
  ) {
    fun orThrow(): GQLValue {
      // Let warnings go through.
      // Especially deprecation warnings are ok.
      if (issues.any { it.severity == Issue.Severity.ERROR }) {
        throw SourceAwareException(issues.first().message, issues.first().sourceLocation)
      }
      return coercedValue
    }
  }

  fun coerce(value: GQLValue, expectedType: GQLType): Result = Result(
      coercedValue = validateAndCoerceInternal(value, expectedType),
      variableReferences = variableReferences,
      issues = issues
  )


  private fun registerIssue(value: GQLValue, expectedType: GQLType) {
    issues.add(Issue.ValidationError(message = "Value `${value.toUtf8()}` cannot be used in position expecting `${expectedType.pretty()}`", sourceLocation = value.sourceLocation))
  }

  private fun validateAndCoerceInputObject(value: GQLValue, expectedTypeDefinition: GQLInputObjectTypeDefinition): GQLValue {
    val expectedType = GQLNamedType(name = expectedTypeDefinition.name)
    if (value !is GQLObjectValue) {
      registerIssue(value, expectedType)
      return value
    }

    // 3.10 All required input fields must have a value
    expectedTypeDefinition.inputFields.forEach { inputValueDefinition ->
      if (inputValueDefinition.type is GQLNonNullType
          && inputValueDefinition.defaultValue == null
          && value.fields.firstOrNull { it.name == inputValueDefinition.name } == null
      ) {
        issues.add(Issue.ValidationError(message = "No value passed for required inputField ${inputValueDefinition.name}", sourceLocation = value.sourceLocation))
      }
    }

    return GQLObjectValue(fields = value.fields.mapNotNull { field ->
      val inputField = expectedTypeDefinition.inputFields.firstOrNull { it.name == field.name }
      if (inputField == null) {
        // 3.10 Input values coercion: extra values are errors
        issues.add(Issue.ValidationError(message = "Field ${field.name} is not defined by ${expectedType.pretty()}", sourceLocation = field.sourceLocation))
        return@mapNotNull null
      }
      GQLObjectField(
          name = field.name,
          value = validateAndCoerceInternal(field.value, inputField.type)
      )
    })
  }

  private fun validateAndCoerceEnum(value: GQLValue, enumTypeDefinition: GQLEnumTypeDefinition): GQLValue {
    val expectedType = GQLNamedType(name = enumTypeDefinition.name)
    if (value !is GQLEnumValue) {
      registerIssue(value, expectedType)
      return value
    }

    val enumValue = enumTypeDefinition.enumValues.firstOrNull { it.name == value.value }
    if (enumValue == null) {
      issues.add(Issue.ValidationError(
          message = "Cannot find enum value `${value.value}` of type `${enumTypeDefinition.name}`",
          sourceLocation = value.sourceLocation
      ))
    } else if (enumValue.isDeprecated()) {
      issues.add(Issue.DeprecatedUsage(
          message = "Use of deprecated enum value `${value.value}` of type `${enumTypeDefinition.name}`",
          sourceLocation = value.sourceLocation
      ))
    }
    return value
  }

  private fun validateAndCoerceScalar(value: GQLValue, expectedType: GQLNamedType): GQLValue {
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


  private fun validateAndCoerceInternal(value: GQLValue, expectedType: GQLType): GQLValue {
    if (value is GQLVariableValue) {
      variableReferences.add(VariableReference(value, expectedType))
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
        return validateAndCoerceInternal(value, expectedType.type)
      }
      is GQLListType -> {
        if (value !is GQLListValue) {
          registerIssue(value, expectedType)
          return value
        }
        return GQLListValue(
            values = value.values.map { validateAndCoerceInternal(it, expectedType.type) }
        )
      }
      is GQLNamedType -> {
        when (val expectedTypeDefinition = schema.typeDefinition(expectedType.name)) {
          is GQLInputObjectTypeDefinition -> {
            return validateAndCoerceInputObject(value, expectedTypeDefinition)
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
            issues.add(Issue.ValidationError("Value cannot be of non-input type ${expectedType.pretty()}", value.sourceLocation))
            return value
          }
        }
      }
    }
  }
}