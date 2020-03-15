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

internal fun Operation.checkVariableReferences() {
  fields.checkVariableReferences(operation = this, filePath = filePath)
}

internal fun Fragment.checkVariableReferences(operation: Operation) {
  try {
    fields.checkVariableReferences(
        operation = operation,
        filePath = filePath
    )
  } catch (e: ParseException) {
    throw GraphQLParseException("$filePath: ${e.message}[${operation.filePath}]")
  }
}

private fun List<Field>.checkVariableReferences(operation: Operation, filePath: String) {
  forEach { field ->
    field.checkVariableReferences(operation = operation, filePath = filePath)
    field.fields.forEach { it.checkVariableReferences(operation = operation, filePath = filePath) }
  }
}

private fun Field.checkVariableReferences(operation: Operation, filePath: String) {
  args.forEach { arg ->
    if (arg.value is Map<*, *> && arg.value["kind"] == "Variable") {
      val variableName = arg.value["variableName"]
      val variable = operation.variables.find { it.name == variableName } ?: throw ParseException(
          message = "Variable `$variableName` is not defined by operation `${operation.operationName}`",
          sourceLocation = arg.sourceLocation
      )

      if (!arg.type.isGraphQLTypeAssignableFrom(variable.type)) {
        throw ParseException(
            message = "Variable `$variableName` of type `${variable.type}` used in position expecting type `${arg.type}`",
            sourceLocation = arg.sourceLocation
        )
      }
    }
  }

  inlineFragments.forEach { fragment ->
    fragment.fields.forEach { field ->
      field.checkVariableReferences(operation = operation, filePath = filePath)
    }
  }

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

  fields.forEach { it.checkVariableReferences(operation = operation, filePath = filePath) }
}

private fun String.isGraphQLTypeAssignableFrom(otherType: String): Boolean {
  var i = 0
  var j = 0
  do {
    when {
      this[i] == otherType[j] -> {
        i++; j++
      }
      otherType[j] == '!' -> j++
      else -> return false
    }
  } while (i < length && j < otherType.length)

  return i == length && (j == otherType.length || (otherType[j] == '!' && j == otherType.length - 1))
}
