package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental


/**
 * @param addKotlinLabsDefinitions automatically import the kotlin_labs definitions, even if no `@link` is present. If [excludeCacheDirectives] is `true`, cache related directives are excluded.
 * @param foreignSchemas a list of known [ForeignSchema] that may or may not be imported depending on the `@link` directives
 * @param excludeCacheDirectives whether to exclude cache related directives when auto-importing the kotlin_labs definitions. Has no effect if [addKotlinLabsDefinitions] is `false`.
 * @param computeKeyFields whether to compute cache key fields. Can be false when using the Apollo Cache compiler plugin to avoid unneeded computation.
 */
@ApolloExperimental
class SchemaValidationOptions
@Deprecated("This constructor was exposed by mistake and will be removed in a future version.", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
constructor(
    val addKotlinLabsDefinitions: Boolean,
    val foreignSchemas: List<ForeignSchema>,
    val excludeCacheDirectives: Boolean,
    val computeKeyFields: Boolean,
    val mergeOptions: MergeOptions,
) {
  class Builder {
    var addKotlinLabsDefinitions: Boolean = false
    val foreignSchemas: MutableList<ForeignSchema> = mutableListOf()
    var excludeCacheDirectives: Boolean = false
    var computeKeyFields: Boolean = true
    var mergeOptions: MergeOptions = MergeOptions.Default

    fun addKotlinLabsDefinitions(addKotlinLabsDefinitions: Boolean) = apply {
      this.addKotlinLabsDefinitions = addKotlinLabsDefinitions
    }

    fun foreignSchemas(schemas: List<ForeignSchema>): Builder = apply {
      foreignSchemas.clear()
      foreignSchemas.addAll(schemas)
    }

    fun addForeignSchema(schema: ForeignSchema): Builder = apply {
      foreignSchemas.add(schema)
    }

    fun excludeCacheDirectives(excludeCacheDirectives: Boolean) = apply {
      this.excludeCacheDirectives = excludeCacheDirectives
    }

    fun computeKeyFields(computeKeyFields: Boolean) = apply {
      this.computeKeyFields = computeKeyFields
    }

    fun mergeOptions(mergeOptions: MergeOptions) = apply {
      this.mergeOptions = mergeOptions
    }

    fun build(): SchemaValidationOptions {
      @Suppress("DEPRECATION_ERROR")
      return SchemaValidationOptions(
          addKotlinLabsDefinitions = addKotlinLabsDefinitions,
          foreignSchemas = foreignSchemas,
          excludeCacheDirectives = excludeCacheDirectives,
          computeKeyFields = computeKeyFields,
          mergeOptions = mergeOptions
      )
    }
  }

  @Deprecated("This constructor was exposed by mistake and will be removed in a future version.", level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
  @Suppress("DEPRECATION_ERROR")
  constructor(
      addKotlinLabsDefinitions: Boolean,
      foreignSchemas: List<ForeignSchema>,
      excludeCacheDirectives: Boolean,
  ) : this(
      addKotlinLabsDefinitions = addKotlinLabsDefinitions,
      foreignSchemas = foreignSchemas,
      excludeCacheDirectives = excludeCacheDirectives,
      computeKeyFields = true,
      mergeOptions = MergeOptions.Default,
  )

  @Deprecated("This constructor was exposed by mistake and will be removed in a future version.", level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
  @Suppress("DEPRECATION_ERROR")
  constructor(
      addKotlinLabsDefinitions: Boolean,
      foreignSchemas: List<ForeignSchema>,
  ) : this(
      addKotlinLabsDefinitions = addKotlinLabsDefinitions,
      foreignSchemas = foreignSchemas,
      excludeCacheDirectives = false,
      computeKeyFields = true,
      mergeOptions = MergeOptions.Default,
  )
}