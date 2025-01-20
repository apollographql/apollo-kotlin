package com.apollographql.apollo.execution

import com.apollographql.apollo.api.Error

/**
 *
 */
sealed interface SubscriptionEvent

/**
 * A response from the stream. May contain field errors.
 */
class SubscriptionResponse(val response: GraphQLResponse) : SubscriptionEvent

/**
 * This subscription failed.
 *
 * This event is terminal and the client can decide whether to retry or give up.
 * For convenience, [SubscriptionError] uses the same error type as the GraphQL errors but these are not in the same domain. Another server
 * implementation could decide to use something else.
 *
 * Example: validation error
 */
class SubscriptionError(val errors: List<Error>) : SubscriptionEvent