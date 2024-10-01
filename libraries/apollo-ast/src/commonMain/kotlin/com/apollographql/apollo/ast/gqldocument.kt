package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.ast.internal.ExtensionsMerger
import com.apollographql.apollo.ast.internal.builtinsDefinitionsStr
import com.apollographql.apollo.ast.internal.ensureSchemaDefinition
import com.apollographql.apollo.ast.internal.kotlinLabsDefinitions_0_3
import com.apollographql.apollo.ast.internal.kotlinLabsDefinitions_0_4
import com.apollographql.apollo.ast.internal.linkDefinitionsStr
import com.apollographql.apollo.ast.internal.nullabilityDefinitionsStr
import okio.Buffer

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

/**
 * Returns a "full schema" document. Full schema documents are for use by clients and other tools that need
 * to know what features are supported by a given server. They include builtin directives and merge all type
 * extensions
 */
@ApolloExperimental
fun GQLDocument.toFullSchemaGQLDocument(): GQLDocument {
  return ensureSchemaDefinition()
      .withDefinitions(builtinDefinitions())
      .toMergedGQLDocument()
}

fun GQLDocument.toSchema(): Schema = validateAsSchema().getOrThrow()

@ApolloExperimental
class MergeOptions(
    val allowFieldNullabilityModification: Boolean
) {
  companion object {
    val Default: MergeOptions = MergeOptions(false)
  }
}

@ApolloExperimental
fun GQLDocument.toMergedGQLDocument(mergeOptions: MergeOptions = MergeOptions.Default): GQLDocument {
  return mergeExtensions(mergeOptions).getOrThrow()
}

@ApolloExperimental
fun GQLDocument.mergeExtensions(mergeOptions: MergeOptions = MergeOptions.Default): GQLResult<GQLDocument> {
  val result = ExtensionsMerger(definitions, mergeOptions).merge()
  return GQLResult(
      GQLDocument(result.value.orEmpty(), sourceLocation = null),
      result.issues
  )
}

@Deprecated("Use GQLDocument.toSDL() to write a GQLDocument")
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

@ApolloInternal
const val KOTLIN_LABS_VERSION = "v0.3"

/**
 * Extra apollo Kotlin specific definitions from https://specs.apollo.dev/kotlin_labs/<[version]>
 */
fun kotlinLabsDefinitions(version: String): List<GQLDefinition> {
  return definitionsFromString(when (version) {
    // v0.3 has no behavior change over v0.2, so both versions map to the same definitions
    "v0.2", "v0.3" -> kotlinLabsDefinitions_0_3
    // v0.4 doesn't have `@nonnull`
    "v0.4" -> kotlinLabsDefinitions_0_4
    else -> error("kotlin_labs/$version definitions are not supported, please use $KOTLIN_LABS_VERSION")
  })
}

/**
 * The foreign schemas supported by Apollo Kotlin.
 * This is exported in case users want to validate documents meant for Apollo Kotlin.
 */
@ApolloExperimental
fun builtinForeignSchemas(): List<ForeignSchema> {
  return listOf(
      ForeignSchema("kotlin_labs", "v0.2", kotlinLabsDefinitions("v0.2"), listOf("optional", "nonnull")),
      ForeignSchema("kotlin_labs", "v0.3", kotlinLabsDefinitions("v0.3"), listOf("optional", "nonnull")),
      ForeignSchema("kotlin_labs", "v0.4", kotlinLabsDefinitions("v0.4"), listOf("optional")),
      ForeignSchema("nullability", "v0.4", nullabilityDefinitions("v0.4"), listOf("catch")),
  )
}

@ApolloInternal
const val NULLABILITY_VERSION = "v0.4"

/**
 * Extra nullability definitions from https://specs.apollo.dev/nullability/<[version]>
 */
fun nullabilityDefinitions(version: String): List<GQLDefinition> {
  return definitionsFromString(when (version) {
    NULLABILITY_VERSION -> nullabilityDefinitionsStr
    else -> error("nullability/$version definitions are not supported, please use $NULLABILITY_VERSION")
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

internal fun combineDefinitions(
    left: List<GQLDefinition>,
    right: List<GQLDefinition>,
    conflictResolution: ConflictResolution,
): List<GQLDefinition> {
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
@ApolloExperimental
fun GQLDocument.toSDL(indent: String = "  "): String {
  val buffer = Buffer()
  val writer = SDLWriter(buffer, indent)

  definitions.forEachIndexed { index, definition ->
    when {
      definition is GQLScalarTypeDefinition && definition.name in GQLTypeDefinition.builtInTypes -> {
        // Always skip scalar definitions, it's a must in the spec
        return@forEachIndexed
      }

      definition is GQLTypeDefinition && definition.name in GQLTypeDefinition.builtInTypes ||
          definition is GQLDirectiveDefinition && definition.name in GQLDirectiveDefinition.builtInDirectives -> {
        writer.write(definition)
      }

      else -> {
        writer.write(definition)
      }
    }
    if (index < definitions.size - 1) {
      writer.write("\n")
    }
  }
  return buffer.readUtf8()
}
