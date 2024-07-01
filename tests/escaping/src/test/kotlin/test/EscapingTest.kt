package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import reserved.WhileQuery
import reserved.type.Do

class EscapingTest {
  // Not actually a test, we're just making sure this compiles
  suspend fun test() {
    val response = ApolloClient.Builder().build()
        .query(WhileQuery(
            `return` = Optional.Present("public"),
            `for` = Do(
                `if` = Optional.Absent,
                yield = Optional.Absent,
            ),
        ))
        .execute()
    val `while`: WhileQuery.While? = response.dataOrThrow().`while`

    @Suppress("UNUSED_VARIABLE")
    val `if`: String? = `while`?.`if`
    @Suppress("UNUSED_VARIABLE")
    val `else`: String? = `while`?.`else`
  }
}
