package com.apollographql.apollo3.subscription

/**
 * Provides instance of [SubscriptionConnectionParams] to be sent to the subscription server after connection is established.
 */
interface SubscriptionConnectionParamsProvider {
  fun provide(): SubscriptionConnectionParams
  class Const(private val params: SubscriptionConnectionParams) : SubscriptionConnectionParamsProvider {
    override fun provide(): SubscriptionConnectionParams {
      return params
    }
  }
}