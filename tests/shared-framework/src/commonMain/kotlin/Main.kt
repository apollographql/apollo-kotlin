import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueError
import com.example.GetFooQuery
import kotlinx.coroutines.runBlocking
import okio.use

@Throws(Exception::class)
fun testInterceptor(interceptor: HttpInterceptor) {
  runBlocking {
    MockServer().use { mockServer ->
      mockServer.enqueueError(500)
      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .addHttpInterceptor(interceptor)
          .build()
          .use { apolloClient ->
            apolloClient.query(GetFooQuery()).execute().apply {
              println("exception: ${exception!!.cause!!.message}")
              check(exception!!.cause!!.message!!.contains("interceptor error"))
            }
          }
    }
  }
}
