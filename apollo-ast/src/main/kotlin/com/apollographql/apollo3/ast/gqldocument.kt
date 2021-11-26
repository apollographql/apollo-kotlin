package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloExperimental
import okio.buffer
import okio.source

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

/**
 * Definitions from the spec
 */
fun builtinDefinitions() = definitionsFromResources("builtins.graphqls")

/**
 * Extra apollo specific definitions
 */
fun apolloDefinitions() = definitionsFromResources("apollo.graphqls")


@OptIn(ApolloExperimental::class)
private fun definitionsFromResources(name: String): List<GQLDefinition> {
  return GQLDocument::class.java.getResourceAsStream("/$name")!!
      .source()
      .buffer()
      .parseAsGQLDocument()
      .valueAssertNoErrors()
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

/**
 * Adds [definitions] to the [GQLDocument]
 *
 * If a definition alreay exists, it is kept as is and a warning is logged
 *
 * See https://spec.graphql.org/draft/#sel-FAHnBPLCAACCcooU
 */
private fun GQLDocument.withDefinitions(definitions: List<GQLDefinition>): GQLDocument {
  val mergedDefinitions = this.definitions.toMutableList()

  definitions.forEach { builtInTypeDefinition ->
    check(builtInTypeDefinition is GQLNamed) {
      "only extra named definitions are supported"
    }
    val existingDefinition = mergedDefinitions.firstOrNull { (it as? GQLNamed)?.name == builtInTypeDefinition.name }
    if (existingDefinition != null) {
      println("Apollo: definition '${builtInTypeDefinition.name}' is already in the schema at " +
          "'${existingDefinition.sourceLocation.filePath}:${existingDefinition.sourceLocation}', skip it")
    } else {
      mergedDefinitions.add(builtInTypeDefinition)
    }
  }

  return copy(
      definitions = mergedDefinitions
  )
}
