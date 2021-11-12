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

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withIdlingResource(idlingResource: ApolloIdlingResource) = newBuilder().idlingResource(idlingResource).build()
