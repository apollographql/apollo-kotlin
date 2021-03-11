package com.apollographql.apollo3.compiler.frontend

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
                error = "Schema defines `$namedType` as root for `$operationType` but `$namedType` is not defined",
                sourceLocation = sourceLocation
            )
      }
}

fun GQLDocument.withoutBuiltinDefinitions(): GQLDocument {
  return copy(
      definitions = definitions.filter {
        (it as? GQLTypeDefinition)?.isBuiltIn() != true
            && (it as? GQLDirectiveDefinition)?.isBuiltIn() != true
      }
  )
}

fun GQLDocument.withBuiltinDefinitions(): GQLDocument {
  val mergedDefinitions = definitions.toMutableList()

  GraphQLParser.builtinTypes().definitions.forEach { builtInTypeDefinition ->
    if (builtInTypeDefinition is GQLNamed && mergedDefinitions.any { (it as? GQLNamed)?.name == builtInTypeDefinition.name }) {
      println("ApolloGraphQL: definition '${builtInTypeDefinition.name}' is already in the schema, skip it")
    } else {
      mergedDefinitions.add(builtInTypeDefinition)
    }
  }

  return copy(
      definitions = mergedDefinitions
  )
}


fun GQLDocument.withBuiltinDirectives(): GQLDocument {
  val mergedDefinitions = definitions.toMutableList()

  GraphQLParser.builtinTypes().definitions
      .filterIsInstance<GQLDirectiveDefinition>()
      .forEach { builtInTypeDefinition ->
    if (mergedDefinitions.any { (it as? GQLNamed)?.name == builtInTypeDefinition.name }) {
      println("ApolloGraphQL: definition '${builtInTypeDefinition.name}' is already in the schema, skip it")
    } else {
      mergedDefinitions.add(builtInTypeDefinition)
    }
  }

  return copy(
      definitions = mergedDefinitions
  )
}

fun GQLDocument.toSchema(): Schema {
  return Schema(
      typeDefinitions = definitions.filterIsInstance<GQLTypeDefinition>().associateBy { it.name },
      queryTypeDefinition = rootOperationTypeDefinition("query") ?: throw SchemaValidationException("No query root type found"),
      mutationTypeDefinition = rootOperationTypeDefinition("mutation"),
      subscriptionTypeDefinition = rootOperationTypeDefinition("subscription"),
      directiveDefinitions = definitions.filterIsInstance<GQLDirectiveDefinition>().associateBy { it.name }
  )
}

private fun String.withIndents(): String {
  var indent = 0
  return lines().joinToString(separator = "\n") { line ->
    if (line.endsWith("}")) indent -= 2
    if (indent < 0) {
      // This happens if a description ends with '}'
      indent = 0
    }
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
