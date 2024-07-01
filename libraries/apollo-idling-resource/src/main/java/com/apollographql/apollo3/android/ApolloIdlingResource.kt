@file:Suppress("DEPRECATION")

package com.apollographql.apollo.android

import androidx.test.espresso.IdlingResource
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.internal.ApolloClientListener

/**
 * An [Espresso](https://developer.android.com/training/testing/espresso/idling-resource) [IdlingResource] that monitors calls to:
 * - [com.apollographql.apollo.ApolloCall.execute]
 * - [com.apollographql.apollo.ApolloCall.toFlow]
 * - [com.apollographql.apollo.ApolloClient.executeAsFlow]
 *
 * [ApolloIdlingResource] is deprecated, and you should wait for your UI to change instead. See [this article about ways to do so](https://medium.com/androiddevelopers/alternatives-to-idling-resources-in-compose-tests-8ae71f9fc473).
 *
 */
@Deprecated("IdlingResource makes tests fragile. How the data is fetched is not relevant to UI tests. Use reactive patterns instead.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
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
/**
 * Registers the given [ApolloIdlingResource] in this [ApolloClient]
 *
 * [ApolloIdlingResource] is deprecated, and you should wait for your UI to change instead. See [this article about ways to do so](https://medium.com/androiddevelopers/alternatives-to-idling-resources-in-compose-tests-8ae71f9fc473).
 */
@Deprecated("IdlingResource makes tests fragile. How the data is fetched is not relevant to UI tests. Use reactive patterns instead.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
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
