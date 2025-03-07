import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
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
          .addHttpInterceptor(object : HttpInterceptor {
            override suspend fun intercept(
                request: HttpRequest,
                chain: HttpInterceptorChain,
            ): HttpResponse {
              //throw DefaultApolloException("interceptor#1 error")
              return chain.proceed(request)
            }

          })
          .build()
          .use { apolloClient ->
            apolloClient.query(GetFooQuery()).execute().apply {
              check(exception!!.message!!.contains("interceptor error"))
            }
          }
    }
  }
}