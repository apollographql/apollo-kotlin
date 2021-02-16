package com.apollographql.apollo3.internal.subscription

import java.util.Collections

class ApolloSubscriptionServerException(val errorPayload: Map<String, Any?>) : ApolloSubscriptionException("Subscription failed")