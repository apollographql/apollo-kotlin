import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.http.HttpInterceptor
import com.example.GetFooQuery
import kotlinx.coroutines.runBlocking
import okio.use

@Throws(Exception::class)
fun testInterceptor(interceptor: HttpInterceptor) {
  runBlocking {
    ApolloClient.Builder()
        .serverUrl("http://unused")
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
