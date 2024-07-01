package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.json.ApolloJsonElement

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