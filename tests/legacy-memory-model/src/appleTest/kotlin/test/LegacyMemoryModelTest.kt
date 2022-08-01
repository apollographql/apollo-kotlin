package test

import com.apollographql.apollo3.ApolloClient
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class LegacyMemoryModelTest {
  @Test
  fun failOnLegacyMemoryManager() {
    val thrown = assertFailsWith<IllegalStateException> {
      ApolloClient.Builder()
    }

    assertContains(thrown.message!!, "The legacy memory manager is no longer supported")
  }
}
