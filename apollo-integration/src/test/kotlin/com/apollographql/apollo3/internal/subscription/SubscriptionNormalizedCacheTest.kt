package com.apollographql.apollo3.internal.subscription

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.ApolloSubscriptionCall
import com.apollographql.apollo3.IdFieldCacheKeyResolver
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.integration.subscription.NewRepoCommentSubscription
import com.apollographql.apollo3.isFromCache
import com.apollographql.apollo3.subscription.OperationClientMessage
import com.apollographql.apollo3.subscription.OperationServerMessage
import com.apollographql.apollo3.subscription.SubscriptionTransport
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class SubscriptionNormalizedCacheTest {
  private lateinit var subscriptionTransportFactory: MockSubscriptionTransportFactory
  private lateinit var apolloClient: ApolloClient
  private lateinit var subscriptionCall: ApolloSubscriptionCall<NewRepoCommentSubscription.Data>
  private lateinit var networkOperationData: Map<String, Any>

  @Before
  fun setUp() {
    subscriptionTransportFactory = MockSubscriptionTransportFactory()
    apolloClient = ApolloClient.builder()
        .serverUrl("http://google.com")
        .dispatcher(TrampolineExecutor())
        .subscriptionTransportFactory(subscriptionTransportFactory)
        .normalizedCache(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdFieldCacheKeyResolver())
        .build()
    subscriptionCall = apolloClient.subscribe(NewRepoCommentSubscription("repo"))
    networkOperationData = mapOf(
        "data" to mapOf(
            "commentAdded" to mapOf(
                "id" to 100,
                "content" to "Network comment content",
                "postedBy" to mapOf(
                    "login" to "user@user.com"
                )
            )
        )
    )
  }

  @Test
  fun `when no cache policy then response not cached`() {
    val callback = SubscriptionManagerCallbackAdapter<NewRepoCommentSubscription.Data>()
    subscriptionCall.execute(callback)

    with(subscriptionTransportFactory.callback!!) {
      onConnected()
      onMessage(OperationServerMessage.ConnectionAcknowledge)
    }

    val uuid = (apolloClient.subscriptionManager as RealSubscriptionManager).subscriptions.keys.first()
    subscriptionTransportFactory.callback?.onMessage(
        OperationServerMessage.Data(uuid.toString(), networkOperationData)
    )

    callback.response.assertResponse(
        expectedFromCache = false,
        expectedContent = "Network comment content"
    )

    val cacheDump = apolloClient.apolloStore.normalizedCache().dump()
    assertThat(NormalizedCache.prettifyDump(cacheDump)).isEqualTo("""
      OptimisticCache {}
      MemoryCache {}
      
    """.trimIndent())
  }

  @Test
  fun `when network only cache policy then response is cached`() {
    val operation = NewRepoCommentSubscription("repo")
    val data = NewRepoCommentSubscription.Data(
        NewRepoCommentSubscription.Data.CommentAdded(
            100, "Cached comment content", NewRepoCommentSubscription.Data.CommentAdded.PostedBy("user@user.com")
        )
    )
    apolloClient.apolloStore.writeOperation(operation, data)

    val callback = SubscriptionManagerCallbackAdapter<NewRepoCommentSubscription.Data>()
    subscriptionCall.cachePolicy(ApolloSubscriptionCall.CachePolicy.NETWORK_ONLY).execute(callback)

    with(subscriptionTransportFactory.callback!!) {
      onConnected()
      onMessage(OperationServerMessage.ConnectionAcknowledge)
    }

    val uuid = (apolloClient.subscriptionManager as RealSubscriptionManager).subscriptions.keys.first()
    subscriptionTransportFactory.callback?.onMessage(
        OperationServerMessage.Data(uuid.toString(), networkOperationData)
    )

    callback.response.assertResponse(
        expectedFromCache = false,
        expectedContent = "Network comment content"
    )

    val cacheDump = apolloClient.apolloStore.normalizedCache().dump()
    assertThat(NormalizedCache.prettifyDump(cacheDump)).isEqualTo("""
      OptimisticCache {}
      MemoryCache {
        "100.postedBy" : {
          "login" : user@user.com
        }

        "100" : {
          "id" : 100
          "content" : Network comment content
          "postedBy" : CacheRecordRef(100.postedBy)
        }
      
        "QUERY_ROOT" : {
          "commentAdded({"repoFullName":"repo"})" : CacheRecordRef(100)
        }
      }
      
    """.trimIndent())
  }

  @Test
  fun `when cache and network policy then first response from cache next one from network`() {
    val operation = NewRepoCommentSubscription("repo")
    val data = NewRepoCommentSubscription.Data(
        NewRepoCommentSubscription.Data.CommentAdded(
            100, "Cached comment content", NewRepoCommentSubscription.Data.CommentAdded.PostedBy("user@user.com")
        )
    )
    apolloClient.apolloStore.writeOperation(operation, data)

    val callback = SubscriptionManagerCallbackAdapter<NewRepoCommentSubscription.Data>()
    subscriptionCall.cachePolicy(ApolloSubscriptionCall.CachePolicy.CACHE_AND_NETWORK).execute(callback)

    callback.response.assertResponse(
        expectedFromCache = true,
        expectedContent = "Cached comment content"
    )

    subscriptionTransportFactory.callback?.onConnected()
    subscriptionTransportFactory.callback?.onMessage(OperationServerMessage.ConnectionAcknowledge)

    val uuid = (apolloClient.subscriptionManager as RealSubscriptionManager).subscriptions.keys.first()
    subscriptionTransportFactory.callback?.onMessage(
        OperationServerMessage.Data(uuid.toString(), networkOperationData)
    )

    callback.response.assertResponse(
        expectedFromCache = false,
        expectedContent = "Network comment content"
    )

    val cacheDump = apolloClient.apolloStore.normalizedCache().dump()
    assertThat(NormalizedCache.prettifyDump(cacheDump)).isEqualTo("""
      OptimisticCache {}
      MemoryCache {
        "100.postedBy" : {
          "login" : user@user.com
        }

        "100" : {
          "id" : 100
          "content" : Network comment content
          "postedBy" : CacheRecordRef(100.postedBy)
        }
      
        "QUERY_ROOT" : {
          "commentAdded({"repoFullName":"repo"})" : CacheRecordRef(100)
        }
      }
      
    """.trimIndent())
  }

  private fun ApolloResponse<NewRepoCommentSubscription.Data>?.assertResponse(expectedFromCache: Boolean, expectedContent: String) {
    assertThat(this).isNotNull()
    assertThat(this!!.isFromCache).isEqualTo(expectedFromCache)
    assertThat(data).isNotNull()
    with(data!!) {
      assertThat(commentAdded!!.id).isEqualTo(100)
      assertThat(commentAdded!!.content).isEqualTo(expectedContent)
      assertThat(commentAdded!!.postedBy!!.login).isEqualTo("user@user.com")
    }
  }
}

private class TrampolineExecutor : AbstractExecutorService() {
  override fun shutdown() {}

  override fun shutdownNow(): List<Runnable> {
    return emptyList()
  }

  override fun isShutdown(): Boolean {
    return false
  }

  override fun isTerminated(): Boolean {
    return false
  }

  override fun awaitTermination(l: Long, timeUnit: TimeUnit): Boolean {
    return false
  }

  override fun execute(runnable: Runnable) {
    runnable.run()
  }
}

private class MockSubscriptionTransportFactory : SubscriptionTransport.Factory {
  var subscriptionTransport: MockSubscriptionTransport? = null
  var callback: SubscriptionTransport.Callback? = null

  override fun create(callback: SubscriptionTransport.Callback): SubscriptionTransport {
    this.callback = callback
    return MockSubscriptionTransport().also { subscriptionTransport = it }
  }
}

private class MockSubscriptionTransport : SubscriptionTransport {
  var lastSentMessage: OperationClientMessage? = null
  var disconnectMessage: OperationClientMessage? = null

  override fun connect() {
  }

  override fun disconnect(message: OperationClientMessage) {
    disconnectMessage = message
  }

  override fun send(message: OperationClientMessage) {
    lastSentMessage = message
  }
}

private class SubscriptionManagerCallbackAdapter<D: Operation.Data> : ApolloSubscriptionCall.Callback<D> {
  var response: ApolloResponse<D>? = null
  var completed = false
  var terminated = false
  var connected = false

  override fun onResponse(response: ApolloResponse<D>) {
    this.response = response
  }

  override fun onFailure(e: ApolloException) {
    throw UnsupportedOperationException()
  }

  override fun onCompleted() {
    completed = true
  }

  override fun onTerminated() {
    terminated = true
  }

  override fun onConnected() {
    connected = true
  }
}
