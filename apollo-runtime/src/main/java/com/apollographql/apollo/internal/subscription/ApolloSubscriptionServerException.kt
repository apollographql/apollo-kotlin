package com.apollographql.apollo.internal.subscription

import com.apollographql.apollo.api.internal.Utils.__checkNotNull
import java.util.Collections

class ApolloSubscriptionServerException(val errorPayload: Map<String, Any?>) : ApolloSubscriptionException("Subscription failed")