package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.ast.internal.ExtensionsMerger
import com.apollographql.apollo.ast.internal.builtinsDefinitionsStr
import com.apollographql.apollo.ast.internal.compilerOptions_0_0
import com.apollographql.apollo.ast.internal.compilerOptions_0_1_additions
import com.apollographql.apollo.ast.internal.kotlinLabsDefinitions_0_3
import com.apollographql.apollo.ast.internal.kotlinLabsDefinitions_0_4
import com.apollographql.apollo.ast.internal.linkDefinitionsStr
import com.apollographql.apollo.ast.internal.nullabilityDefinitionsStr
import okio.Buffer

/**
 * Add built-in definitions supported by Apollo Kotlin to the [GQLDocument]
 *
 * SDL representations must skip scalars and may skip directive definitions. This function adds them back to form a full schema.
 *
 * If a definition already exists, it is kept as is
 *
 * Scalars: https://spec.graphql.org/draft/#sel-GAHXJHABAB_D4G
 * Directives: https://spec.graphql.org/draft/#sel-FAHnBPLCAACCcooU
 */
@Deprecated("use toFullSchemaGQLDocument instead", ReplaceWith("toFullSchemaGQLDocument()"), level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_1_2)
fun GQLDocument.withBuiltinDefinitions(): GQLDocument {
  return copy(
      definitions = definitions.withBuiltinDefinitions()
  )
}

/**
 * This is called 3 times in Apollo Kotlin:
 * 1. In codegen, to make sure we have built-in scalar types.
 * 2. When converting a schema from SDL to introspection, because the SDL might be a backend SDL.
 * 3. When downloading a schema from the registry, because the API doesn't return the scalar type definitions.
 *
 * TODO v5: only add scalar definitions, and not everything.
 */
internal fun List<GQLDefinition>.withBuiltinDefinitions(): List<GQLDefinition> {
  return combineDefinitions(this, builtinDefinitions())
}

/**
 * Returns a "full schema" document containing the built-in definitions supported by Apollo Kotlin.
 *
 * Using this is usually dangerous as there's a risk of disconnect between the spec version used by
 * the server and the one used by Apollo Kotlin. Whenever possible, it is best to generate a gull
 * schema from introspection results.
 *
 * See https://github.com/graphql/graphql-wg/blob/main/rfcs/FullSchemas.md
 */
@ApolloExperimental
fun GQLDocument.toFullSchemaGQLDocument(): GQLDocument {
  @Suppress("DEPRECATION_ERROR")
  return withBuiltinDefinitions()
}

fun GQLDocument.toSchema(): Schema = validateAsSchema().getOrThrow()

@ApolloExperimental
class MergeOptions(
    val allowAddingDirectivesToExistingFieldDefinitions: Boolean,
    val allowFieldNullabilityModification: Boolean
) {
  companion object {
    val Default: MergeOptions = MergeOptions(false, false)
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

@Deprecated("Use GQLDocument.toSDL() to write a GQLDocument", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun GQLDocument.withoutBuiltinDefinitions(): GQLDocument {
  return withoutDefinitions(builtinDefinitions())
}

/**
 * Definitions tracking the specification draft
 */
fun builtinDefinitions() = definitionsFromString(builtinsDefinitionsStr)

/**
 * The @link definition for bootstrapping
 *
 * https://specs.apollo.dev/link/v1.0/
 */
@ApolloInternal
fun linkDefinitions() = definitionsFromString(linkDefinitionsStr)

/**
 * We used to auto import kotlin_labs definitions so that users could use `@nonnull`, etc...
 * Moving forward, we require users to explicitly import the version of the kotlin_labs definitions
 * they are using.
 *
 * TODO v5 remove this
 */
@ApolloInternal
const val AUTO_IMPORTED_KOTLIN_LABS_VERSION = "v0.3"

/**
 * The latest supported version of the `kotlin_labs` definitions
 */
@ApolloInternal
const val KOTLIN_LABS_VERSION = "v0.4"

/**
 * Apollo Kotlin definitions from https://specs.apollo.dev/kotlin_labs/<[version]>
 */
@ApolloInternal
fun kotlinLabsDefinitions(version: String): List<GQLDefinition> {
  return definitionsFromString(when (version) {
    // v0.3 has no behavior change over v0.2, so both versions map to the same definitions
    "v0.2", "v0.3" -> kotlinLabsDefinitions_0_3
    // v0.4 doesn't have `@nonnull`
    "v0.4" -> kotlinLabsDefinitions_0_4
    // v0.5 adds `@map` and `@mapTo`
    "v0.5" -> kotlinLabsDefinitions_0_4 + compilerOptions_0_0
    // v0.6 adds `@generateDataBuilders`
    "v0.6" -> kotlinLabsDefinitions_0_4 + compilerOptions_0_0 + compilerOptions_0_1_additions
    else -> error("kotlin_labs/$version definitions are not supported, please use $AUTO_IMPORTED_KOTLIN_LABS_VERSION")
  })
}

internal val autoLinkedKotlinLabsForeignSchema = ForeignSchema("kotlin_labs", "v0.3", kotlinLabsDefinitions("v0.3"), listOf("optional", "nonnull"))

/**
 * The foreign schemas supported by Apollo Kotlin.
 * This is exported in case users want to validate documents meant for Apollo Kotlin.
 */
@ApolloExperimental
fun builtinForeignSchemas(): List<ForeignSchema> {
  return listOf(
      ForeignSchema("kotlin_labs", "v0.2", kotlinLabsDefinitions("v0.2"), listOf("optional", "nonnull")),
      autoLinkedKotlinLabsForeignSchema,
      ForeignSchema("kotlin_labs", "v0.4", kotlinLabsDefinitions("v0.4"), listOf("optional")),
      ForeignSchema("kotlin_labs", "v0.5", kotlinLabsDefinitions("v0.5"), listOf("optional")),
      ForeignSchema("kotlin_labs", "v0.6", kotlinLabsDefinitions("v0.6"), listOf("optional")),
      ForeignSchema("nullability", "v0.4", nullabilityDefinitions("v0.4"), listOf("catch")),
      ForeignSchema("kotlin_compiler_options", "v0.1", definitionsFromString(compilerOptions_0_0 + compilerOptions_0_1_additions), emptyList())
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

/**
 * Left biased union of [left] and [right]
 */
internal fun combineDefinitions(
    left: List<GQLDefinition>,
    right: List<GQLDefinition>,
): List<GQLDefinition> {
  val mergedDefinitions = left.toMutableList()

  right.forEach { builtInTypeDefinition ->
    check(builtInTypeDefinition is GQLNamed) {
      "only extra named definitions are supported"
    }
    val existingDefinition = mergedDefinitions.firstOrNull {
      builtInTypeDefinition::class == it::class
          && (it as? GQLNamed)?.name == builtInTypeDefinition.name
    }
    if (existingDefinition == null) {
      mergedDefinitions.add(builtInTypeDefinition)
    }
  }

  return mergedDefinitions
}


/**
 * Outputs a schema document to SDL. For executable documents, use toUtf8()
 *
 * SDL representations must skip scalars definitions.
 *
 * See https://spec.graphql.org/draft/#sel-GAHXJHABAB_D4G
 */
@ApolloExperimental
fun GQLDocument.toSDL(indent: String = "  ", includeBuiltInScalarDefinitions: Boolean = false): String {
  val buffer = Buffer()
  val writer = SDLWriter(buffer, indent)

  definitions.forEachIndexed { index, definition ->
    when {
      definition is GQLScalarTypeDefinition
          && definition.name in GQLTypeDefinition.builtInTypes
          && !includeBuiltInScalarDefinitions -> {
        // Always skip scalar definitions, it's a must in the spec
        return@forEachIndexed
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

@ApolloExperimental
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_1_2)
@Deprecated("This is only kept for backward compatibility reasons. Use the overload instead.", level = DeprecationLevel.HIDDEN)
fun GQLDocument.toSDL(indent: String = "  ") = toSDL(indent, false)
