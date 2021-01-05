package com.apollographql.apollo.subscription

import java.util.HashMap

/**
 * Represents connection parameters to be sent after connection with subscription server is established.
 * Note: provided connection parameters will be sent as JSON string.
 */
class SubscriptionConnectionParams : HashMap<String, Any?> {
  constructor() : super()
  constructor(m: Map<String, Any?>) : super(m)
}

