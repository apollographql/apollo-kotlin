package termination

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.network.okHttpClient
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

fun main() = runBlocking {
  val okHttpClient = OkHttpClient.Builder().build()
  val apolloClient = ApolloClient.Builder()
      .serverUrl("https://www.google.com")
      .okHttpClient(okHttpClient)
      .build()

  try {
    val response = apolloClient.query(GetRandomQuery()).execute()
    println("random = ${response.data?.random}")
  } catch (e: ApolloHttpException) {
    e.printStackTrace()
  }

  apolloClient.dispose()
  okHttpClient.dispatcher.executorService.shutdown()
  okHttpClient.connectionPool.evictAll()

  println("done")
}