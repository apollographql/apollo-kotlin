package com.apollographql.apollo3.ast

import com.apollographql.apollo3.ast.internal.apollo_v0_1_definitionsStr
import com.apollographql.apollo3.ast.internal.apollo_v0_2_definitionsStr
import com.apollographql.apollo3.ast.internal.builtinsDefinitionsStr
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.ast.internal.linkDefinitionsStr
import okio.Buffer

fun GQLDocument.withBuiltinDefinitions(): GQLDocument {
  return withDefinitions(builtinDefinitions())
}

fun GQLDocument.withoutBuiltinDefinitions(): GQLDocument {
  return withoutDefinitions(builtinDefinitions())
}

/**
 * Definitions from the spec
 */
fun builtinDefinitions() = definitionsFromString(builtinsDefinitionsStr)

/**
 * The @link definition for bootstrapping
 *
 * https://specs.apollo.dev/link/v1.0/
 */
fun linkDefinitions() = definitionsFromString(linkDefinitionsStr)

@Deprecated("Use apolloDefinitions(version) instead", ReplaceWith("apolloDefinitions(\"v0.1\")"))
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_5_1)
fun apolloDefinitions() = definitionsFromString(apollo_v0_1_definitionsStr)

/**
 * Extra apollo specific definitions from https://specs.apollo.dev/kotlin_labs/<[version]>
 */
fun apolloDefinitions(version: String): List<GQLDefinition> {
  return definitionsFromString(when(version) {
    "v0.1" -> apollo_v0_1_definitionsStr
    "v0.2" -> apollo_v0_2_definitionsStr
    else -> error("Apollo definitions $version are not supported")
  })
}

private fun definitionsFromString(string: String): List<GQLDefinition> {
  return Buffer().writeUtf8(string)
      .parseAsGQLDocument(null)
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
            "'${existingDefinition.sourceLocation?.filePath}:${existingDefinition.sourceLocation}'")
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
