import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import com.apollographql.apollo.testing.internal.runTest
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import test.FooQuery
import kotlin.test.Test
import kotlin.test.assertEquals

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