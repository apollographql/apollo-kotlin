package com.apollographql.apollo.internal.subscription

import com.apollographql.apollo.api.internal.Utils.__checkNotNull
import com.apollographql.apollo.internal.subscription.ApolloSubscriptionException
import java.util.Collections

class ApolloSubscriptionServerException(errorPayload: Map<String, Any>) : ApolloSubscriptionException("Subscription failed") {
  @JvmField
  val errorPayload: Map<String, Any> = Collections.unmodifiableMap(__checkNotNull(errorPayload, "errorPayload == null"))
}