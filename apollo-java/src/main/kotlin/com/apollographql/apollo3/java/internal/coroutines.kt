package com.apollographql.apollo3.java.internal

import com.apollographql.apollo3.java.Subscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

internal fun launchToSubscription(block: suspend () -> Unit): Subscription {
  val job = coroutineScope.launch {
    block()
  }
  return Subscription { job.cancel() }
}
