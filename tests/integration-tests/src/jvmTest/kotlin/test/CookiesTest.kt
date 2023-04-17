package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.okHttpClient
import com.apollographql.apollo3.testing.internal.runTest
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.junit.Test
import kotlin.test.assertEquals

class CookiesTest {
  /**
   * A very crude [CookieJar] that does no synchronization and uses the host as a key
   * Only use for tests.
   */
  class ObservableCookieJar : CookieJar {
    private val storage = mutableMapOf<String, List<Cookie>>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
      storage.merge(url.host, cookies) { old, new ->
        old + new
      }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
      return storage[url.host] ?: listOf()
    }
  }

  @Test
  fun cookiesArePersisted() = runTest {
    val mockServer = MockServer()
    val cookieJar = ObservableCookieJar()
    val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .build()

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .okHttpClient(okHttpClient)
        .build()

    val json = buildJsonString {
      HeroNameQuery().adapter().toJson(this, ScalarAdapters.Empty, HeroNameQuery.Data(hero = HeroNameQuery.Hero(name = "Luke")))
    }

    mockServer.enqueue(MockResponse.Builder().body(json).addHeader("Set-Cookie", "yummy_cookie=choco").build())

    // first query should set the cookie
    apolloClient.query(HeroNameQuery()).execute()
    // consume the first request
    mockServer.takeRequest()

    mockServer.enqueue(json)
    // first query should send the cookie
    apolloClient.query(HeroNameQuery()).execute()

    val cookie = mockServer.takeRequest().headers["Cookie"]
    assertEquals("yummy_cookie=choco", cookie)
  }
}
