package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.testing.internal.runTest
import kotlinx.coroutines.flow.first
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals

class ApolloClientBuilderTest {

  @Test
  fun NotSettingUrlFailsWithAnExplicitError() = runTest {
    ApolloClient.Builder()
        .build()
        .use {
          val flow = it.query(FooQuery()).toFlow()
          try {
            flow.first()
          } catch (e: IllegalStateException) {
            assertEquals(e.message?.contains("ApolloRequest.url is missing for request 'FooOperation', did you call ApolloClient.Builder.url(url)"), true)
          }
        }
  }
}