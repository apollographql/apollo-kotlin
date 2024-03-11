package com.apollographql.apollo3.network.ws.incubating

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.json.ApolloJsonElement

@ApolloExperimental
interface SubscriptionParser<D : Operation.Data> {
  fun parse(payload: ApolloJsonElement): ApolloResponse<D>?
}

@ApolloExperimental
interface SubscriptionParserFactory {
  fun <D: Operation.Data> createParser(request: ApolloRequest<D>): SubscriptionParser<D>
}