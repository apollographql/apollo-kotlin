package com.apollographql.apollo.internal.subscription

import com.apollographql.apollo.exception.ApolloException

open class ApolloSubscriptionException : ApolloException {
  constructor(message: String?) : super(message!!)
  constructor(message: String?, cause: Throwable?) : super(message!!, cause!!)
}