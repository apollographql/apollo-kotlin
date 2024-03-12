package com.apollographql.apollo3.network.ws.incubating

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.json.ApolloJsonElement

/**
 * A [SubscriptionParser] transforms JSON responses contained in WebSocket messages into parsed [ApolloResponse]
 */
@ApolloExperimental
interface SubscriptionParser<D : Operation.Data> {
  fun parse(response: ApolloJsonElement): ApolloResponse<D>?
}

/**
 * A factory for [SubscriptionParser]
 */
@ApolloExperimental
interface SubscriptionParserFactory {
  fun <D: Operation.Data> createParser(request: ApolloRequest<D>): SubscriptionParser<D>
}