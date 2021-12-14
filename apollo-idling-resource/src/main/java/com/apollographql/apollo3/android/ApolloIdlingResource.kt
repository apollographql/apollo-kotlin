package com.apollographql.apollo3.android
import androidx.test.espresso.IdlingResource
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import kotlinx.coroutines.flow.Flow
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

  companion object {
    @JvmStatic
    @Deprecated(
        message = "Used for backward compatibility with 2.x. You now need to pass your ApolloIdlingResource to your ApolloClient.Builder." +
            " See https://www.apollographql.com/docs/android/migration/3.0/ for more details.",
        ReplaceWith("ApolloIdlingResource(name)")
    )
    @Suppress("UNUSED_PARAMETER")
    fun create(name: String, apolloClient: ApolloClient) {
      throw NotImplementedError()
    }
  }
}

fun ApolloClient.Builder.idlingResource(idlingResource: ApolloIdlingResource): ApolloClient.Builder {
  check (!interceptors.any { it is IdlingResourceInterceptor }) { "idlingResource was already set, can only be set once" }
  return addInterceptor(IdlingResourceInterceptor(idlingResource))
}

private class IdlingResourceInterceptor(private val idlingResource: ApolloIdlingResource): ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    // Do not update the idling resource on subscriptions as they will never terminate
    return if (request.operation !is Subscription) {
      chain.proceed(request).onStart {
        idlingResource.operationStart()
      }.onCompletion {
        idlingResource.operationEnd()
      }
    } else {
      chain.proceed(request)
    }
  }
}
