package com.apollographql.apollo3.graphql.ast

import com.apollographql.apollo3.graphql.ast.GraphQLParser.apolloDefinitions
import com.apollographql.apollo3.graphql.ast.GraphQLParser.builtinDefinitions
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

fun GQLDocument.withoutExtraDefinitions(): GQLDocument {
  val extraDefinitions = (builtinDefinitions() + apolloDefinitions()).map {
    check (it is GQLNamed)
    it.name
  }.toSet()

  return copy(
      definitions = definitions.filter {
        if (it !is GQLNamed) {
          // Can this happen?
          return@filter true
        }

        !extraDefinitions.contains(it.name)
      }
  )
}

fun GQLDocument.withExtraDefinitions(warn: Boolean = true): GQLDocument {
  val mergedDefinitions = definitions.toMutableList()

  (builtinDefinitions() + apolloDefinitions()).forEach { builtInTypeDefinition ->
    check (builtInTypeDefinition is GQLNamed) {
      "only extra named definitions are supported"
    }
    val existingDefinition = mergedDefinitions.firstOrNull { (it as? GQLNamed)?.name == builtInTypeDefinition.name }
    if (existingDefinition != null ) {
      if (warn) {
        println("ApolloGraphQL: definition '${builtInTypeDefinition.name}' is already in the schema at " +
            "'${existingDefinition.sourceLocation.filePath}:${existingDefinition.sourceLocation}', skip it")
      }
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
