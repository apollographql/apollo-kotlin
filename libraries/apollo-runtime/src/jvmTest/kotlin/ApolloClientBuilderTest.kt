import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import okhttp3.OkHttpClient
import kotlin.test.Test

class ApolloClientBuilderTest {
  @Test
  fun allowSettingOkHttpClientOnNewBuilder() {
    val apolloClient1 = ApolloClient.Builder()
        .serverUrl("http://localhost:8080")
        .okHttpClient(OkHttpClient())
        .build()

    @Suppress("UNUSED_VARIABLE")
    val apolloClient2 = apolloClient1.newBuilder()
        .okHttpClient(OkHttpClient())
        .build()
  }
}