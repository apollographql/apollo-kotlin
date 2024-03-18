package com.apollographql.apollo3.android

import androidx.test.espresso.IdlingResource
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.internal.ApolloClientListener

class ApolloIdlingResource(
    private val resourceName: String,
) : IdlingResource {

  private var activeCalls = 0
  private var callback: IdlingResource.ResourceCallback? = null

  @Synchronized
  fun operationStart() {
    activeCalls++
  }

  @Synchronized
  fun operationEnd() {
    activeCalls--
    if (activeCalls == 0) {
      callback?.onTransitionToIdle()
    }
  }

  override fun getName(): String {
    return resourceName
  }

  override fun isIdleNow(): Boolean {
    return activeCalls == 0
  }

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
    this.callback = callback
  }
}

fun ApolloClient.Builder.idlingResource(idlingResource: ApolloIdlingResource): ApolloClient.Builder {
  return addListener(IdlingResourceListener(idlingResource))
}

private class IdlingResourceListener(private val idlingResource: ApolloIdlingResource) : ApolloClientListener {
  override fun requestStarted(request: ApolloRequest<*>) {
    idlingResource.operationStart()
  }

  override fun requestCompleted(request: ApolloRequest<*>) {
    idlingResource.operationEnd()
  }
}
