package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import okio.buffer
import okio.source

fun GQLDocument.withBuiltinDefinitions(): GQLDocument {
  return withDefinitions(builtinDefinitions())
}

fun GQLDocument.withoutBuiltinDefinitions(): GQLDocument {
  return withoutDefinitions(builtinDefinitions())
}

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
@Deprecated("This method is deprecated and will be removed in a future version")
fun GQLDocument.withBuiltinDirectives(): GQLDocument {
  return withDefinitions(builtinDefinitions().filterIsInstance<GQLDirectiveDefinition>())
}

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
@Deprecated("This method is deprecated and will be removed in a future version")
fun GQLDocument.withoutBuiltinDirectives(): GQLDocument {
  return withoutDefinitions(builtinDefinitions().filterIsInstance<GQLDirectiveDefinition>())
}

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
@Deprecated("This method is deprecated and will be removed in a future version")
fun GQLDocument.withApolloDefinitions(): GQLDocument {
  @Suppress("DEPRECATION")
  return withDefinitions(apolloDefinitions())
}

/**
 * Definitions from the spec
 */
fun builtinDefinitions() = definitionsFromResources("builtins.graphqls")

/**
 * The @link definition for bootstrapping
 *
 * https://specs.apollo.dev/link/v1.0/
 */
fun linkDefinitions() = definitionsFromResources("link.graphqls")

/**
 * Extra apollo specific definitions from https://specs.apollo.dev/kotlin_labs/v0.1
 */
fun apolloDefinitions() = definitionsFromResources("apollo.graphqls")

private fun definitionsFromResources(name: String): List<GQLDefinition> {
  return GQLDocument::class.java.getResourceAsStream("/$name")!!
      .source()
      .buffer()
      .parseAsGQLDocument("($name)")
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

internal enum class ConflictResolution {
  /**
   * If a definition exists in both left and right, throw an error
   */
  Error,
  //MergeIfSameDefinition,
  /**
   * If a definition exists in both left and right, use left always
   */
  TakeLeft
}

internal fun combineDefinitions(left: List<GQLDefinition>, right: List<GQLDefinition>, conflictResolution: ConflictResolution): List<GQLDefinition> {
  val mergedDefinitions = left.toMutableList()

  right.forEach { builtInTypeDefinition ->
    check(builtInTypeDefinition is GQLNamed) {
      "only extra named definitions are supported"
    }
    val existingDefinition = mergedDefinitions.firstOrNull { (it as? GQLNamed)?.name == builtInTypeDefinition.name }
    if (existingDefinition != null) {
      if (conflictResolution == ConflictResolution.Error) {
        error("Apollo: definition '${builtInTypeDefinition.name}' is already in the schema at " +
            "'${existingDefinition.sourceLocation.filePath}:${existingDefinition.sourceLocation}'")
      }
    } else {
      mergedDefinitions.add(builtInTypeDefinition)
    }
  }

  return mergedDefinitions
}

/**
 * Adds [definitions] to the [GQLDocument]
 *
 * If a definition already exists, it is kept as is and a warning is logged
 * See https://spec.graphql.org/draft/#sel-FAHnBPLCAACCcooU
 *
 * A better implementation might verify that the definitions match or are compatible
 */
private fun GQLDocument.withDefinitions(definitions: List<GQLDefinition>): GQLDocument {
  return copy(
      definitions = combineDefinitions(this.definitions, definitions, ConflictResolution.TakeLeft)
  )
}
