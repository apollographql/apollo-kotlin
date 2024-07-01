package test

import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.composeJsonRequest
import com.apollographql.apollo.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.toApolloResponse
import com.example.GetRandomQuery
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ApolloInternal
class NoRuntimeTest {
  companion object {
    private lateinit var sampleServer: SampleServer

    @BeforeClass
    @JvmStatic
    fun beforeClass() {
      sampleServer = SampleServer()
    }

    @AfterClass
    @JvmStatic
    fun afterClass() {
      sampleServer.close()
    }
  }

  @Test
  fun noRuntime() {
    val query = GetRandomQuery()

    val okHttpClient = OkHttpClient()

    val request = Request.Builder().url(sampleServer.graphqlUrl())
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
      BufferedSourceJsonReader(it.source()).toApolloResponse(operation = query)
    }

    assertEquals(42, apolloResponse?.data?.random)
  }
}
