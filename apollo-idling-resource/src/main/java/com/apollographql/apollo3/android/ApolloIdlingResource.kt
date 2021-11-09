package com.apollographql.apollo3.android
import androidx.test.espresso.IdlingResource
import com.apollographql.apollo3.ApolloClient
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

class ApolloIdlingResource(
    private val resourceName: String,
): IdlingResource {

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
  return addFlowDecorator {
    it.onStart {
      idlingResource.operationStart()
    }.onCompletion {
      idlingResource.operationEnd()
    }
  }
}

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withIdlingResource(idlingResource: ApolloIdlingResource) = newBuilder().idlingResource(idlingResource).build()
