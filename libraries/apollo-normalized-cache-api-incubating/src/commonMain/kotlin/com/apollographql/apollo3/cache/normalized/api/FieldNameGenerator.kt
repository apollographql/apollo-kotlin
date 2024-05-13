package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable

/**
 * Called to get a field's name to use within its parent [Record].
 *
 * This is useful for instance to exclude certain pagination arguments when storing a connection field.
 */
@ApolloExperimental
interface FieldNameGenerator {
  /**
   * Returns the field name to use within its parent [Record].
   */
  fun getFieldName(context: FieldNameContext): String
}

/**
 * Context passed to the [FieldNameGenerator.getFieldName] method.
 */
@ApolloExperimental
class FieldNameContext(
    val parentType: String,
    val field: CompiledField,
    val variables: Executable.Variables,
)

/**
 * Default [FieldNameGenerator] that returns the field name with its arguments, excluding pagination arguments defined with the
 * `@fieldPolicy(forField: "...", paginationArgs: "...")` directive.
 */
@ApolloExperimental
object DefaultFieldNameGenerator : FieldNameGenerator {
  override fun getFieldName(context: FieldNameContext): String {
    return context.field.nameWithArguments(context.variables)
  }
}

/**
 * A [FieldNameGenerator] that generates field names excluding
 * [Relay connection types](https://relay.dev/graphql/connections.htm#sec-Connection-Types) pagination arguments.
 */
@ApolloExperimental
class ConnectionFieldNameGenerator(private val connectionFields: Map<String, List<String>>) : FieldNameGenerator {
  companion object {
    private val paginationArguments = setOf("first", "last", "before", "after")
  }

  override fun getFieldName(context: FieldNameContext): String {
    return if (context.field.name in connectionFields[context.parentType].orEmpty()) {
      context.field.newBuilder()
          .arguments(
              context.field.arguments.filter { argument ->
                argument.name !in paginationArguments
              }
          )
          .build()
          .nameWithArguments(context.variables)
    } else {
      DefaultFieldNameGenerator.getFieldName(context)
    }
  }
}
