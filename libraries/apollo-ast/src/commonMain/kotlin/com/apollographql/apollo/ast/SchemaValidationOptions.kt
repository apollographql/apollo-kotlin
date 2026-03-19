package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental

/**
 * @property addBuiltinDefinitions add the builtin definitions. [addBuiltinDefinitions] allows validation of source schemas that don't contain builtin definitions. If null (default),
 * only the missing definitions are added.
 * @property foreignSchemas a list of known [ForeignSchema] that may or may not be imported depending on the `@link` directives
 * @property computeKeyFields whether to compute cache key fields. Can be false when using the Apollo Cache compiler plugin to avoid unneeded computation.
 * @property mergeOptions the options to use when merging extensions.
 */
@ApolloExperimental
class SchemaValidationOptions internal constructor(
    val addBuiltinDefinitions: Boolean?,
    val foreignSchemas: List<ForeignSchema>,
    val computeKeyFields: Boolean,
    val mergeOptions: MergeOptions,
) {
  @Deprecated("This constructor was exposed by mistake and will be removed in a future version.", level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
  constructor(
      addKotlinLabsDefinitions: Boolean,
      foreignSchemas: List<ForeignSchema>,
      computeKeyFields: Boolean,
      mergeOptions: MergeOptions,
  ) : this(null, foreignSchemas, computeKeyFields, mergeOptions)

  class Builder {
    @Deprecated("addKotlinLabsDefinitions() has no effect anymore. If you need to use the kotlin_labs directives, use the appropriate `@link` directive before calling this funcion.", level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
    var addKotlinLabsDefinitions: Boolean = false

    /**
     * TODO: [addBuiltinDefinitions] should be false by default.
     */
    var addBuiltinDefinitions: Boolean? = null
    val foreignSchemas: MutableList<ForeignSchema> = mutableListOf()
    var computeKeyFields: Boolean = true
    var mergeOptions: MergeOptions = MergeOptions.Default

    @Deprecated("addKotlinLabsDefinitions() has no effect anymore. If you need to use the kotlin_labs directives, use the appropriate `@link` directive before calling this funcion.", level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
    fun addKotlinLabsDefinitions(addKotlinLabsDefinitions: Boolean) = apply {
      @Suppress("DEPRECATION_ERROR")
      this.addKotlinLabsDefinitions = addKotlinLabsDefinitions
    }

    fun foreignSchemas(schemas: List<ForeignSchema>): Builder = apply {
      foreignSchemas.clear()
      foreignSchemas.addAll(schemas)
    }

    fun addForeignSchema(schema: ForeignSchema): Builder = apply {
      foreignSchemas.add(schema)
    }

    fun computeKeyFields(computeKeyFields: Boolean) = apply {
      this.computeKeyFields = computeKeyFields
    }

    fun mergeOptions(mergeOptions: MergeOptions) = apply {
      this.mergeOptions = mergeOptions
    }

    fun addBuiltinDefinitions(addBuiltinDefinitions: Boolean?) = apply {
      this.addBuiltinDefinitions = addBuiltinDefinitions
    }

    fun build(): SchemaValidationOptions {
      @Suppress("DEPRECATION_ERROR")
      return SchemaValidationOptions(
          addBuiltinDefinitions = addBuiltinDefinitions,
          foreignSchemas = foreignSchemas,
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
  ) : this(
      addBuiltinDefinitions = true,
      foreignSchemas = foreignSchemas,
      computeKeyFields = true,
      mergeOptions = MergeOptions.Default,
  )
}