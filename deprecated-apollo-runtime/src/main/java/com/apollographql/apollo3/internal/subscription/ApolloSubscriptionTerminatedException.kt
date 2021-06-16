package com.apollographql.apollo3.internal.subscription

import com.apollographql.apollo3.api.exception.ApolloException

class ApolloSubscriptionTerminatedException : ApolloException {
  constructor(message: String?) : super(message!!)
  constructor(message: String?, cause: Throwable?) : super(message!!, cause!!)
}