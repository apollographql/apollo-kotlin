package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.MapJsonWriter
import com.apollographql.apollo3.api.json.writeObject
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import optionals.java.MyQuery
import optionals.java.type.MyInput
import org.junit.Test
import java.util.Optional
import java.util.Optional.empty
import kotlin.test.assertEquals

@Suppress("UNCHECKED_CAST")
class JavaOptionalsTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  private suspend fun setUp() {
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .build()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }


  @Test
  fun serializeVariablesPresent() {
    val query = MyQuery(
        /* nullableInt = */ Optional.of(Optional.of(0)),
        /* nonNullableInt = */ 1,
        /* nonNullableIntWithDefault = */ Optional.of(2),
        /* nullableInput = */ Optional.of(Optional.of(myInputPresent())),
        /* nonNullableInput = */ myInputPresent(),
        /* nonNullableInputWithDefault = */ Optional.of(myInputPresent()),
    )
    val mapJsonWriter = MapJsonWriter()
    mapJsonWriter.writeObject {
      query.serializeVariables(this, CustomScalarAdapters.Empty)
    }
    val jsonMap = mapJsonWriter.root() as Map<String, Any?>
    assertEquals(mapOf(
        "nullableInt" to 0,
        "nonNullableInt" to 1,
        "nonNullableIntWithDefault" to 2,
        "nullableInput" to mapOf(
            "nullableInt" to 3,
            "nonNullableInt" to 4,
            "nonNullableIntWithDefault" to 5,
        ),
        "nonNullableInput" to mapOf(
            "nullableInt" to 3,
            "nonNullableInt" to 4,
            "nonNullableIntWithDefault" to 5,
        ),
        "nonNullableInputWithDefault" to mapOf(
            "nullableInt" to 3,
            "nonNullableInt" to 4,
            "nonNullableIntWithDefault" to 5,
        ),
    ), jsonMap)
  }

  @Test
  fun serializeVariablesAbsent() {
    val query = MyQuery(
        /* nullableInt = */ empty(),
        /* nonNullableInt = */ 1,
        /* nonNullableIntWithDefault = */ empty(),
        /* nullableInput = */ empty(),
        /* nonNullableInput = */ myInputAbsent(),
        /* nonNullableInputWithDefault = */ empty(),
    )
    val mapJsonWriter = MapJsonWriter()
    mapJsonWriter.writeObject {
      query.serializeVariables(this, CustomScalarAdapters.Empty)
    }
    val jsonMap = mapJsonWriter.root() as Map<String, Any?>
    assertEquals(mapOf(
        "nonNullableInt" to 1,
        "nonNullableInput" to mapOf(
            "nonNullableInt" to 4,
        ),
    ), jsonMap)
  }

  @Test
  fun serializeVariablesMixed() {
    val query = MyQuery(
        /* nullableInt = */ Optional.of(empty()),
        /* nonNullableInt = */ 1,
        /* nonNullableIntWithDefault = */ Optional.of(2),
        /* nullableInput = */ Optional.of(empty()),
        /* nonNullableInput = */ myInputMixed(),
        /* nonNullableInputWithDefault = */ Optional.of(myInputMixed()),
    )
    val mapJsonWriter = MapJsonWriter()
    mapJsonWriter.writeObject {
      query.serializeVariables(this, CustomScalarAdapters.Empty)
    }
    val jsonMap = mapJsonWriter.root() as Map<String, Any?>
    assertEquals(mapOf(
        "nullableInt" to null,
        "nonNullableInt" to 1,
        "nonNullableIntWithDefault" to 2,
        "nullableInput" to null,
        "nonNullableInput" to mapOf(
            "nullableInt" to null,
            "nonNullableInt" to 4,
            "nonNullableIntWithDefault" to 5,
        ),
        "nonNullableInputWithDefault" to mapOf(
            "nullableInt" to null,
            "nonNullableInt" to 4,
            "nonNullableIntWithDefault" to 5,
        ),
    ), jsonMap)
  }

  @Test
  fun readResult() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = MyQuery(empty(), 0, empty(), empty(), myInputAbsent(), empty())
    mockServer.enqueue("""
      {
        "data": {
          "nullableInt": null,
          "nonNullableInt": 1,
          "nullableMyType": null,
          "nonNullableMyType": {
            "nullableInt": null,
            "nonNullableInt": 2
          }
        }
      }
    """)
    var result = apolloClient.query(query).execute()
    assertEquals(
        MyQuery.Data(
            /* nullableInt = */ empty(),
            /* nonNullableInt = */ 1,
            /* nullableMyType = */ empty(),
            /* nonNullableMyType = */
            MyQuery.NonNullableMyType(
                /* nullableInt = */ empty(),
                /* nonNullableInt = */ 2,
            ),
        ),
        result.dataAssertNoErrors, message = null
    )

    mockServer.enqueue("""
      {
        "data": {
          "nullableInt": 0,
          "nonNullableInt": 1,
          "nullableMyType": {
            "nullableInt": 2,
            "nonNullableInt": 3
          },
          "nonNullableMyType": {
            "nullableInt": null,
            "nonNullableInt": 4
          }
        }
      }
    """)
    result = apolloClient.query(query).execute()
    assertEquals(
        MyQuery.Data(
            /* nullableInt = */ Optional.of(0),
            /* nonNullableInt = */ 1,
            /* nullableMyType = */
            Optional.of(MyQuery.NullableMyType(
                /* nullableInt = */ Optional.of(2),
                /* nonNullableInt = */ 3,
            )),
            /* nonNullableMyType = */
            MyQuery.NonNullableMyType(
                /* nullableInt = */ empty(),
                /* nonNullableInt = */ 4,
            ),
        ),
        result.dataAssertNoErrors, message = null
    )
  }


  private fun myInputPresent() = MyInput(
      /* nullableInt = */ Optional.of(Optional.of(3)),
      /* nonNullableInt = */ 4,
      /* nonNullableIntWithDefault = */ Optional.of(5),
  )

  private fun myInputAbsent() = MyInput(
      /* nullableInt = */ empty(),
      /* nonNullableInt = */ 4,
      /* nonNullableIntWithDefault = */ empty(),
  )

  private fun myInputMixed() = MyInput(
      /* nullableInt = */ Optional.of(empty()),
      /* nonNullableInt = */ 4,
      /* nonNullableIntWithDefault = */ Optional.of(5),
  )

}
