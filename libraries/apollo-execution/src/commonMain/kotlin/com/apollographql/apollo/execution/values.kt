package com.apollographql.apollo.execution

import com.apollographql.apollo.api.Error
import com.apollographql.apollo.ast.GQLBooleanValue
import com.apollographql.apollo.ast.GQLEnumValue
import com.apollographql.apollo.ast.GQLFloatValue
import com.apollographql.apollo.ast.GQLIntValue
import com.apollographql.apollo.ast.GQLListValue
import com.apollographql.apollo.ast.GQLNullValue
import com.apollographql.apollo.ast.GQLObjectValue
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLValue
import com.apollographql.apollo.ast.GQLVariableValue
import kotlinx.coroutines.Deferred

/**
 * An internal value
 * - Numbers are either Int, Double or the result of custom scalar coercion (see below)
 * - Enums are Strings
 * - May contain the internal representation of custom scalars (java.util.Date, kotlin.Long, etc...)
 * - Input objects are Maps
 * - Output objects are Maps
 *
 * Note: the return value of the resolvers are not considered [InternalValue] and are never coerced to [ExternalValue].
 * Only their fields are
 */
typealias InternalValue = Any?

/**
 * The result of a resolver
 *
 * - an [InternalValue] for leaf types
 * - an opaque value for composite types (doesn't have to be a map)
 * - a [List] for list types
 */
typealias ResolverValue = Any?

/**
 * Any of [ResolverValue] or [Error]
 */
internal typealias ResolverValueOrError = Any?

/**
 * Any of [com.apollographql.apollo.api.json.ApolloJsonElement] or [Error]
 */
typealias ExternalValue = Any?

/**
 * Any of [ExternalValue] or [Deferred]<[ExternalValue]>
 */
internal typealias ExternalValueOrDeferred = Any?

internal suspend fun ExternalValueOrDeferred.finalize(errors: MutableList<Error>): ExternalValue {
  return when (this) {
    is Deferred<*> -> await()?.finalize(errors)

    is Error -> {
      errors.add(this)
      null
    }

    is Map<*, *> -> mapValues { it.value?.finalize(errors) }
    is List<*> -> map { it?.finalize(errors) }
    else -> this
  }
}
