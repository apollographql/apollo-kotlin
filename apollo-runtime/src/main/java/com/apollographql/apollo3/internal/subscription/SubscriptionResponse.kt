package com.apollographql.apollo3.internal.subscription

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Subscription

class SubscriptionResponse<D : Operation.Data>(val subscription: Subscription<D>, val response: ApolloResponse<D>)