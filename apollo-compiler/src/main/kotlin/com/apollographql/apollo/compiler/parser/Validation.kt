package com.apollographql.apollo.compiler.parser

import com.apollographql.apollo.compiler.PackageNameProvider
import com.apollographql.apollo.compiler.ir.*

internal fun List<Operation>.checkMultipleOperationDefinitions(packageNameProvider: PackageNameProvider) {
  groupBy { packageNameProvider.operationPackageName(it.filePath) + it.operationName }
      .values
      .find { it.size > 1 }
      ?.last()
      ?.run {
        throw GraphQLParseException("$filePath: There can be only one operation named `$operationName`")
      }
}

internal fun List<Fragment>.checkMultipleFragmentDefinitions() {
  groupBy { it.fragmentName }
      .values
      .find { it.size > 1 }
      ?.last()
      ?.run { throw GraphQLParseException("$filePath: There can be only one fragment named `$fragmentName`") }
}

internal fun Operation.validateArguments(schema: Schema) {
  try {
    fields.validateArguments(operation = this, schema = schema)
  } catch (e: ParseException) {
    throw GraphQLDocumentParseException(
        message = e.message!!,
        sourceLocation = e.sourceLocation,
        graphQLFilePath = filePath
    )
  }
}

internal fun Fragment.validateArguments(operation: Operation, schema: Schema) {
  try {
    fields.validateArguments(operation = operation, schema = schema)
  } catch (e: ParseException) {
    throw GraphQLDocumentParseException(
        message = "${e.message!!}\nOperation `${operation.operationName}` declaration [${operation.filePath}]",
        sourceLocation = e.sourceLocation,
        graphQLFilePath = filePath
    )
  }
}

private fun List<Field>.validateArguments(operation: Operation, schema: Schema) {
  forEach { field ->
    field.validateArguments(operation = operation, schema = schema)
    field.fields.forEach { it.validateArguments(operation = operation, schema = schema) }
  }
}

private fun Field.validateArguments(operation: Operation, schema: Schema) {
  args.forEach { arg ->
    try {
      val argumentTypeRef = schema.resolveType(arg.type)
      argumentTypeRef.validateArgumentValue(
          fieldName = arg.name,
          value = arg.value to true,
          operation = operation,
          schema = schema
      )
    } catch (e: GraphQLParseException) {
      throw ParseException(
          message = e.message!!,
          sourceLocation = arg.sourceLocation
      )
    }
  }

  inlineFragments.forEach { fragment ->
    fragment.fields.forEach { field ->
      field.validateArguments(operation = operation, schema = schema)
    }
  }
  fields.forEach { it.validateArguments(operation = operation, schema = schema) }

  validateConditions(operation)
}

private fun Field.validateConditions(operation: Operation) {
  conditions.forEach { condition ->
    val variable = operation.variables.find { it.name == condition.variableName } ?: throw ParseException(
        message = "Variable `${condition.variableName}` is not defined by operation `${operation.operationName}`",
        sourceLocation = condition.sourceLocation
    )

    val scalarType = ScalarType.forName(variable.type.removeSuffix("!"))
    if (scalarType != ScalarType.BOOLEAN) {
      throw ParseException(
          message = "Variable `${variable.name}` of type `${variable.type}` used in position expecting type `Boolean!`",
          sourceLocation = condition.sourceLocation
      )
    }
  }
}

@Suppress("NAME_SHADOWING")
private fun Schema.TypeRef.validateArgumentValue(
    fieldName: String,
    value: Pair<Any?, Boolean>,
    operation: Operation,
    schema: Schema
) {
  val (value, defined) = value
  when (kind) {
    Schema.Kind.NON_NULL -> {
      if (value == null) {
        if (defined) {
          throw GraphQLParseException("Input field `$fieldName` is non optional")
        }
      } else if (value is Map<*, *>) {
        val variableName = value.extractVariableName()
        if (variableName != null) {
          operation.validateVariableType(
              name = variableName,
              expectedType = this,
              schema = schema
          )
        } else {
          ofType?.validateArgumentValue(
              fieldName = fieldName,
              operation = operation,
              value = value to true,
              schema = schema
          )
        }
      } else {
        ofType?.validateArgumentValue(
            fieldName = fieldName,
            operation = operation,
            value = value to true,
            schema = schema
        )
      }
    }

    Schema.Kind.ENUM,
    Schema.Kind.SCALAR -> {
      if (value is Map<*, *>) {
        val variableName = value.extractVariableName()
        if (variableName != null) {
          operation.validateVariableType(
              name = variableName,
              expectedType = this,
              schema = schema
          )
        } else {
          throw GraphQLParseException("Expected scalar value for input field `$fieldName`, but found object")
        }
      } else if (value is List<*>) {
        throw GraphQLParseException("Expected scalar value for input field `$fieldName`, but found list")
      }
    }

    Schema.Kind.INPUT_OBJECT -> {
      if (value is Map<*, *>) {
        val variableName = value.extractVariableName()
        if (variableName != null) {
          operation.validateVariableType(
              name = variableName,
              expectedType = this,
              schema = schema
          )
        } else {
          (schema[name] as Schema.Type.InputObject).inputFields.forEach { field ->
            field.type.validateArgumentValue(
                fieldName = "$fieldName.${field.name}",
                value = value[field.name] to value.containsKey(field.name),
                operation = operation,
                schema = schema
            )
          }
        }
      } else if (value is List<*>) {
        throw GraphQLParseException("Expected input object value for input field `$fieldName`, but found list")
      } else if (value != null) {
        throw GraphQLParseException("Expected input object value for input field `$fieldName`, but found scalar")
      }
    }

    Schema.Kind.LIST -> {
      if (value is List<*>) {
        value.forEach { item ->
          ofType?.validateArgumentValue(
              fieldName = fieldName,
              value = item to true,
              operation = operation,
              schema = schema
          )
        }
      } else if (value is Map<*, *>) {
        val variableName = value.extractVariableName()
        if (variableName != null) {
          operation.validateVariableType(
              name = variableName,
              expectedType = this,
              schema = schema
          )
        } else {
          throw GraphQLParseException("Expected array value for input field `$fieldName`, but found object")
        }
      } else if (value != null) {
        throw GraphQLParseException("Expected array value for input field `$fieldName`, but found scalar")
      }
    }

    else -> throw GraphQLParseException("Unsupported input field `$fieldName` type `${asGraphQLType()}`")
  }
}

private fun Map<*, *>.extractVariableName(): String? {
  return if (this["kind"] == "Variable") {
    this["variableName"] as String
  } else {
    null
  }
}

private fun Operation.validateVariableType(name: String, expectedType: Schema.TypeRef, schema: Schema) {
  val variable = variables.find { it.name == name } ?: throw GraphQLParseException(
      "Variable `$name` is not defined by operation `${operationName}`"
  )
  val variableType = schema.resolveType(variable.type)
  if (!expectedType.isAssignableFrom(other = variableType, schema = schema)) {
    throw GraphQLParseException(
        "Variable `$name` of type `${variableType.asGraphQLType()}` used in position expecting type `${expectedType.asGraphQLType()}`"
    )
  }
}
