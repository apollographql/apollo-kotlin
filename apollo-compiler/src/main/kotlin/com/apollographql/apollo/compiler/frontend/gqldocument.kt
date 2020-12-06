package com.apollographql.apollo.compiler.frontend

import okio.Buffer
import okio.buffer
import okio.sink
import java.io.File


internal fun GQLDocument.rootOperationTypeDefinition(operationType: String): GQLObjectTypeDefinition? {
  val schemaDefinition = definitions.filterIsInstance<GQLSchemaDefinition>()
      .firstOrNull()
  if (schemaDefinition == null) {
    // 3.3.1
    // No schema definition, look for an object type named after the operationType
    // i.e. Query, Mutation, ...
    return definitions.filterIsInstance<GQLObjectTypeDefinition>()
        .firstOrNull { it.name == operationType.capitalize() }
  }
  return schemaDefinition.rootOperationTypeDefinitions.firstOrNull {
    it.operationType == operationType
  }?.namedType
      ?.let { namedType ->
        definitions.filterIsInstance<GQLObjectTypeDefinition>()
            .firstOrNull { it.name == namedType }
            ?: throw SchemaValidationException(
                message = "Schema defines `$namedType` as root for `$operationType` but `$namedType` is not defined",
                sourceLocation = sourceLocation
            )
      }
}

fun GQLDocument.withoutBuiltinTypes(): GQLDocument {
  return copy(
      definitions = definitions.filter {
        ((it as? GQLTypeDefinition)?.isBuiltIn() == true).not()
      }
  )
}

fun GQLDocument.withBuiltinTypes(): GQLDocument {
  return copy(
      definitions = definitions + GraphQLParser.builtinTypes().definitions
  )
}

fun GQLDocument.toSchema(): Schema {
  return Schema(
      typeDefinitions = definitions.filterIsInstance<GQLTypeDefinition>().associateBy { it.name },
      queryTypeDefinition = rootOperationTypeDefinition("query") ?: throw SchemaValidationException("No query root type found"),
      mutationTypeDefinition = rootOperationTypeDefinition("mutation"),
      subscriptionTypeDefinition = rootOperationTypeDefinition("subscription")
  )
}



private fun String.withIndents(): String {
  var indent = 0
  return lines().joinToString(separator = "\n") { line ->
    if (line.endsWith("}")) indent -= 2
    (" ".repeat(indent) + line).also {
      if (line.endsWith("{")) indent += 2
    }
  }
}

fun GQLNode.toUtf8WithIndents(): String {
  // TODO("stream the indents")
  val buffer = Buffer()
  write(buffer)
  return buffer.readUtf8().withIndents()
}

fun GQLNode.toUtf8(): String {
  val buffer = Buffer()
  write(buffer)
  return buffer.readUtf8()
}

fun GQLNode.toFile(file: File) = file.outputStream().sink().buffer().use {
  write(it)
}
