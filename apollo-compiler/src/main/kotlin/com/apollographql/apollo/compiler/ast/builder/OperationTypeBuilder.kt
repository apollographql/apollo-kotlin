package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.ast.*
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.Operation
import com.apollographql.apollo.compiler.sha256

internal fun Operation.ast(
    operationClassName: String,
    context: Context
): OperationType {
  val dataTypeRef = context.addObjectType(typeName = "Data") {
    ObjectType.Object(
        className = "Data",
        schemaName = "Data",
        fields = fields.map { it.ast(context) },
        fragmentsType = null
    )
  }
  val operationType = when {
    isQuery() -> OperationType.Type.QUERY
    isMutation() -> OperationType.Type.MUTATION
    isSubscription() -> OperationType.Type.SUBSCRIPTION
    else -> throw IllegalArgumentException("Unsupported GraphQL operation type: $operationType")
  }
  return OperationType(
      name = operationClassName,
      type = operationType,
      operationName = operationName,
      operationId = (sourceWithFragments ?: "").filter { it != '\n' }.sha256(),
      queryDocument = sourceWithFragments!!,
      variables = InputType(
          name = "Variables",
          description = "",
          fields = variables.map { variable ->
            InputType.Field(
                name = variable.name.decapitalize().escapeKotlinReservedWord(),
                schemaName = variable.name,
                type = resolveFieldType(
                    graphQLType = variable.type,
                    enums = context.enums,
                    customTypeMap = context.customTypeMap,
                    typesPackageName = context.typesPackageName
                ),
                isOptional = variable.optional(),
                defaultValue = null,
                description = ""
            )
          }
      ),
      data = dataTypeRef,
      nestedObjects = context.objectTypes,
      filePath = filePath
  )
}