package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.ast.internal.apollo_v0_1_definitionsStr
import com.apollographql.apollo3.ast.internal.apollo_v0_2_definitionsStr
import com.apollographql.apollo3.ast.internal.builtinsDefinitionsStr
import com.apollographql.apollo3.ast.internal.linkDefinitionsStr

/**
 * Add builtin definitions from the latest spec version to the [GQLDocument]
 *
 * SDL representations must skip scalars and may skip directive definitions. This function adds them back to form a full schema.
 *
 * If a definition already exists, it is kept as is
 *
 * Scalars: https://spec.graphql.org/draft/#sel-GAHXJHABAB_D4G
 * Directives: https://spec.graphql.org/draft/#sel-FAHnBPLCAACCcooU
 */
fun GQLDocument.withBuiltinDefinitions(): GQLDocument {
  return withDefinitions(builtinDefinitions())
}

@Deprecated("Use GQLDocument.toSDL() to write a GQLDocument without the scalar directives")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
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
  return string
      .parseAsGQLDocument()
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
private fun GQLDocument.withDefinitions(definitions: List<GQLDefinition>): GQLDocument {
  return copy(
      definitions = combineDefinitions(this.definitions, definitions, ConflictResolution.TakeLeft)
  )
}

/**
 * Outputs a schema document to SDL. For executable documents, use toUtf8()
 *
 * SDL representations must skip scalars definitions.
 *
 * See https://spec.graphql.org/draft/#sel-GAHXJHABAB_D4G
 */
fun GQLDocument.toSDL(indent: String = "  "): String {
  return this.copy(
      definitions = definitions.filter {
        it !is GQLScalarTypeDefinition || it.name !in GQLTypeDefinition.builtInTypes
      }
  ).toUtf8(indent)
}
