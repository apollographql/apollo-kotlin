package com.apollographql.apollo.compiler.parser.graphql

import com.apollographql.apollo.compiler.PackageNameProvider
import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.Fragment
import com.apollographql.apollo.compiler.ir.Operation
import com.apollographql.apollo.compiler.ir.ScalarType
import com.apollographql.apollo.compiler.ir.SourceLocation
import com.apollographql.apollo.compiler.parser.error.DocumentParseException
import com.apollographql.apollo.compiler.parser.error.ParseException
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.parser.introspection.asGraphQLType
import com.apollographql.apollo.compiler.parser.introspection.isAssignableFrom
import com.apollographql.apollo.compiler.parser.introspection.resolveType

internal fun List<Operation>.checkMultipleOperationDefinitions(packageNameProvider: PackageNameProvider) {
  groupBy { packageNameProvider.operationPackageName(it.filePath) + it.operationName }
      .values
      .find { it.size > 1 }
      ?.last()
      ?.run {
        throw ParseException("$filePath: There can be only one operation named `$operationName`")
      }
}

internal fun List<Fragment>.checkMultipleFragmentDefinitions() {
  groupBy { it.fragmentName }
      .values
      .find { it.size > 1 }
      ?.last()
      ?.run { throw ParseException("$filePath: There can be only one fragment named `$fragmentName`") }
}

internal fun Operation.validateArguments(schema: IntrospectionSchema) {
  try {
    fields.validateArguments(operation = this, schema = schema)
  } catch (e: ParseException) {
    throw DocumentParseException(
        message = e.message!!,
        sourceLocation = e.sourceLocation,
        filePath = filePath
    )
  }
}

internal fun Fragment.validateArguments(operation: Operation, schema: IntrospectionSchema) {
  try {
    fields.validateArguments(operation = operation, schema = schema)
  } catch (e: ParseException) {
    throw DocumentParseException(
        message = "${e.message!!}\nOperation `${operation.operationName}` declaration [${operation.filePath}]",
        sourceLocation = if (e.sourceLocation == SourceLocation.UNKNOWN) sourceLocation else e.sourceLocation,
        filePath = filePath ?: ""
    )
  }
}

private fun List<Field>.validateArguments(operation: Operation, schema: IntrospectionSchema) {
  forEach { field ->
    field.validateArguments(operation = operation, schema = schema)
    field.fields.forEach { it.validateArguments(operation = operation, schema = schema) }
  }
}

private fun Field.validateArguments(operation: Operation, schema: IntrospectionSchema) {
  args.forEach { arg ->
    try {
      val argumentTypeRef = schema.resolveType(arg.type)
      argumentTypeRef.validateArgumentValue(
          fieldName = arg.name,
          value = arg.value to true,
          operation = operation,
          schema = schema
      )
    } catch (e: ParseException) {
      throw ParseException(
          message = e.message!!,
          sourceLocation = if (e.sourceLocation == SourceLocation.UNKNOWN) arg.sourceLocation else e.sourceLocation
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
private fun IntrospectionSchema.TypeRef.validateArgumentValue(
    fieldName: String,
    value: Pair<Any?, Boolean>,
    operation: Operation,
    schema: IntrospectionSchema
) {
  val (value, defined) = value
  when (kind) {
    IntrospectionSchema.Kind.NON_NULL -> {
      if (value == null) {
        if (defined) {
          throw ParseException("Input field `$fieldName` is non optional")
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

    IntrospectionSchema.Kind.ENUM,
    IntrospectionSchema.Kind.SCALAR -> {
      if (value is Map<*, *>) {
        val variableName = value.extractVariableName()
        if (variableName != null) {
          operation.validateVariableType(
              name = variableName,
              expectedType = this,
              schema = schema
          )
        } else {
          throw ParseException("Expected scalar value for input field `$fieldName`, but found object")
        }
      } else if (value is List<*>) {
        throw ParseException("Expected scalar value for input field `$fieldName`, but found list")
      }
    }

    IntrospectionSchema.Kind.INPUT_OBJECT -> {
      if (value is Map<*, *>) {
        val variableName = value.extractVariableName()
        if (variableName != null) {
          operation.validateVariableType(
              name = variableName,
              expectedType = this,
              schema = schema
          )
        } else {
          (schema[name] as IntrospectionSchema.Type.InputObject).inputFields.forEach { field ->
            field.type.validateArgumentValue(
                fieldName = "$fieldName.${field.name}",
                value = value[field.name] to value.containsKey(field.name),
                operation = operation,
                schema = schema
            )
          }
        }
      } else if (value is List<*>) {
        throw ParseException("Expected input object value for input field `$fieldName`, but found list")
      } else if (value != null) {
        throw ParseException("Expected input object value for input field `$fieldName`, but found scalar")
      }
    }

    IntrospectionSchema.Kind.LIST -> {
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
          throw ParseException("Expected array value for input field `$fieldName`, but found object")
        }
      } else if (value != null) {
        throw ParseException("Expected array value for input field `$fieldName`, but found scalar")
      }
    }

    else -> throw ParseException("Unsupported input field `$fieldName` type `${asGraphQLType()}`")
  }
}

private fun Map<*, *>.extractVariableName(): String? {
  return if (this["kind"] == "Variable") {
    this["variableName"] as String
  } else {
    null
  }
}

private fun Operation.validateVariableType(name: String, expectedType: IntrospectionSchema.TypeRef, schema: IntrospectionSchema) {
  val variable = variables.find { it.name == name } ?: throw ParseException(
      "Variable `$name` is not defined by operation `${operationName}`"
  )
  val variableType = schema.resolveType(variable.type)
  if (!expectedType.isAssignableFrom(other = variableType, schema = schema)) {
    throw ParseException(
        "Variable `$name` of type `${variableType.asGraphQLType()}` used in position expecting type `${expectedType.asGraphQLType()}`"
    )
  }
}
