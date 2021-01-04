package com.apollographql.apollo.internal.subscription

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.cache.normalized.Record

class SubscriptionResponse<D : Operation.Data>(val subscription: Subscription<D>, val response: Response<D>,
                                                val cacheRecords: Collection<Record>)