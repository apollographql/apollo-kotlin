package com.apollographql.apollo3.internal.subscription

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Response
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.cache.normalized.Record

class SubscriptionResponse<D : Operation.Data>(val subscription: Subscription<D>, val response: Response<D>)