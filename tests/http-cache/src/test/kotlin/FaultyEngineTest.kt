import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.network.http.CacheUrlOverrideInterceptor
import com.apollographql.apollo.network.http.HttpEngine
import com.apollographql.apollo.network.okHttpClient
import httpcache.GetRandomQuery
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import okio.Buffer
import okio.IOException
import okio.Source
import okio.Timeout
import okio.buffer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FaultySource : Source {

  private val data = Buffer().writeUtf8("{ \"data\":")

  override fun close() {

  }

  override fun read(sink: Buffer, byteCount: Long): Long {
    val remaining = data.size.coerceAtMost(byteCount)
    if (remaining == 0L) {
      throw IOException("failed to read")
    }
    data.read(sink, remaining)

    return remaining
  }

  override fun timeout(): Timeout {
    return Timeout.NONE
  }
}

class FaultyEngine : HttpEngine {
  val requests = mutableListOf<HttpRequest>()

  override suspend fun execute(request: HttpRequest): HttpResponse {
    requests.add(request)

    return HttpResponse.Builder(200)
        .body(FaultySource().buffer())
        .addHeader("content-length", "30")
        .build()
  }
}

class FaultyEngineTest {

  @Test
  fun httpErrorsAreNotCached() = runBlocking {
    val engine = FaultyEngine()

    val dir = File("build/httpCache")
    dir.deleteRecursively()

    val apolloClient = ApolloClient.Builder()
        .addInterceptor(CacheUrlOverrideInterceptor("http://localhost/graphql"))
        .serverUrl("https://unused.com")
        .httpEngine(engine)
        .build()

    apolloClient.query(GetRandomQuery()).execute().apply {
      assertIs<ApolloNetworkException>(exception)
    }
    apolloClient.query(GetRandomQuery()).execute().apply {
      assertIs<ApolloNetworkException>(exception)
    }

    assertEquals(2, engine.requests.size)
  }
}