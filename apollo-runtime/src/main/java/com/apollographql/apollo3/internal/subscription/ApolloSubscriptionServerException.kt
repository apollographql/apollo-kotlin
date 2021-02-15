package com.apollographql.apollo.internal.subscription

import java.util.Collections

class ApolloSubscriptionServerException(val errorPayload: Map<String, Any?>) : ApolloSubscriptionException("Subscription failed")