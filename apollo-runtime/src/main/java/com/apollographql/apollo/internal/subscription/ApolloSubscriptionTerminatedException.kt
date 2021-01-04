package com.apollographql.apollo.internal.subscription

import com.apollographql.apollo.exception.ApolloException

class ApolloSubscriptionTerminatedException : ApolloException {
  constructor(message: String?) : super(message!!)
  constructor(message: String?, cause: Throwable?) : super(message!!, cause!!)
}