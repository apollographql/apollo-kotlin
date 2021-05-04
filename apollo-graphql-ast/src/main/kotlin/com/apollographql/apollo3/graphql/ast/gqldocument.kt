package com.apollographql.apollo3.graphql.ast

import okio.buffer
import okio.source

/**
 * Validates the given document as a schema. The document should not contain any builtin types
 *
 * The current validation is very simple and will only catch very simple errors
 */
fun GQLDocument.validateAsSchema() = SchemaValidationScope().validate(this)

/**
 * Validates the given document as an executable document.
 */
fun GQLDocument.validateAsOperations(schema: Schema): List<Issue> {
  val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }

  val validationIssues = ExecutableValidationScope(schema, fragments).validate(this)

  val duplicateFragmentIssues = definitions.filterIsInstance<GQLFragmentDefinition>().checkDuplicateFragments()
  val duplicateOperationIssues = definitions.filterIsInstance<GQLOperationDefinition>().checkDuplicateOperations()

  return validationIssues + duplicateFragmentIssues + duplicateOperationIssues
}


fun GQLDocument.withBuiltinDefinitions(): GQLDocument {
  return withDefinitions(builtinDefinitions())
}

fun GQLDocument.withoutBuiltinDefinitions(): GQLDocument {
  return withoutDefinitions(builtinDefinitions())
}

fun GQLDocument.withBuiltinDirectives(): GQLDocument {
  return withDefinitions(builtinDefinitions().filterIsInstance<GQLDirectiveDefinition>())
}

fun GQLDocument.withoutBuiltinDirectives(): GQLDocument {
  return withoutDefinitions(builtinDefinitions().filterIsInstance<GQLDirectiveDefinition>())
}

fun GQLDocument.withApolloDefinitions(): GQLDocument {
  return withDefinitions(apolloDefinitions())
}

fun GQLDocument.toSchema(): Schema {
  return Schema(this)
}

/**
 * Definitions from the spec
 */
fun builtinDefinitions() = definitionsFromResources("builtins.graphqls")

/**
 * Extra apollo specific definitions
 */
fun apolloDefinitions() = definitionsFromResources("apollo.graphqls")


private fun definitionsFromResources(name: String): List<GQLDefinition> {
  return GQLDocument::class.java.getResourceAsStream("/$name")!!
      .source()
      .buffer()
      .parseAsGraphQLDocument()
      .getOrThrow()
      .definitions
}

private fun GQLDocument.withoutDefinitions(definitions: List<GQLDefinition>): GQLDocument {
  val excludedNames = definitions.map {
    check(it is GQLNamed)
    it.name
  }.toSet()

  return copy(
      definitions = this.definitions.filter {
        if (it !is GQLNamed) {
          // GQLSchemaDefinition is not a GQLNamed
          return@filter true
        }

        !excludedNames.contains(it.name)
      }
  )
}

private fun GQLDocument.withDefinitions(definitions: List<GQLDefinition>): GQLDocument {
  val mergedDefinitions = definitions.toMutableList()

  definitions.forEach { builtInTypeDefinition ->
    check(builtInTypeDefinition is GQLNamed) {
      "only extra named definitions are supported"
    }
    val existingDefinition = mergedDefinitions.firstOrNull { (it as? GQLNamed)?.name == builtInTypeDefinition.name }
    if (existingDefinition != null) {
      println("ApolloGraphQL: definition '${builtInTypeDefinition.name}' is already in the schema at " +
          "'${existingDefinition.sourceLocation.filePath}:${existingDefinition.sourceLocation}', skip it")
    } else {
      mergedDefinitions.add(builtInTypeDefinition)
    }
  }

  return copy(
      definitions = mergedDefinitions
  )
}

private fun List<GQLFragmentDefinition>.checkDuplicateFragments(): List<Issue> {
  val filtered = mutableMapOf<String, GQLFragmentDefinition>()
  val issues = mutableListOf<Issue>()

  forEach {
    val existing = filtered.putIfAbsent(it.name, it)
    if (existing != null) {
      issues.add(Issue.ValidationError(
          message = "Fragment ${it.name} is already defined",
          sourceLocation = it.sourceLocation,
      ))
    }
  }
  return issues
}

private fun List<GQLOperationDefinition>.checkDuplicateOperations(): List<Issue> {
  val filtered = mutableMapOf<String, GQLOperationDefinition>()
  val issues = mutableListOf<Issue>()

  forEach {
    if (it.name == null) {
      issues.add(Issue.ValidationError(
          message = "Apollo does not support anonymous operations",
          sourceLocation = it.sourceLocation,
      ))
      return@forEach
    }
    val existing = filtered.putIfAbsent(it.name, it)
    if (existing != null) {
      issues.add(Issue.ValidationError(
          message = "Operation ${it.name} is already defined",
          sourceLocation = it.sourceLocation,
      ))
    }
  }
  return issues
}

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