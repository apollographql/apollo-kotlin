package test

import com.apollographql.apollo.sample.server.DefaultApplication
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.composeJsonRequest
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.example.GetRandomQuery
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ApolloInternal
class NoRuntimeTest {
  companion object {
    private lateinit var context: ConfigurableApplicationContext

    @BeforeClass
    @JvmStatic
    fun beforeClass() {
      context = runApplication<DefaultApplication>()
    }

    @AfterClass
    @JvmStatic
    fun afterClass() {
      context.close()
    }
  }

  @Test
  fun noRuntime() {
    val query = GetRandomQuery()

    val okHttpClient = OkHttpClient()

    val request = Request.Builder().url("http://localhost:8080/graphql")
        .post(object: RequestBody() {
          override fun contentType() = "application/json".toMediaType()

          override fun writeTo(sink: BufferedSink) {
            query.composeJsonRequest(BufferedSinkJsonWriter(sink))
          }

        })
        .build()

    val response = okHttpClient.newCall(request).execute()

    assertTrue(response.isSuccessful)
    assertTrue(response.body != null)

    val apolloResponse = response.body?.use {
      query.parseJsonResponse(BufferedSourceJsonReader(it.source()))
    }

    assertEquals(42, apolloResponse?.data?.random)
  }
}
