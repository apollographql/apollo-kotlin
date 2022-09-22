package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Optional.Absent
import com.apollographql.apollo3.api.Optional.Present
import com.apollographql.apollo3.api.json.MapJsonWriter
import com.apollographql.apollo3.api.json.writeObject
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import optionals.apollo.MyQuery
import optionals.apollo.type.MyInput
import org.junit.Test
import kotlin.test.assertEquals

@Suppress("UNCHECKED_CAST")
class ApolloOptionalsTest {
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
        /* nullableInt = */ Present(Present(0)),
        /* nonNullableInt = */ 1,
        /* nonNullableIntWithDefault = */ Present(2),
        /* nullableInput = */ Present(Present(myInputPresent())),
        /* nonNullableInput = */ myInputPresent(),
        /* nonNullableInputWithDefault = */ Present(myInputPresent()),
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
        /* nullableInt = */ Absent,
        /* nonNullableInt = */ 1,
        /* nonNullableIntWithDefault = */ Absent,
        /* nullableInput = */ Absent,
        /* nonNullableInput = */ myInputAbsent(),
        /* nonNullableInputWithDefault = */ Absent,
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
        /* nullableInt = */ Present(Absent),
        /* nonNullableInt = */ 1,
        /* nonNullableIntWithDefault = */ Present(2),
        /* nullableInput = */ Present(Absent),
        /* nonNullableInput = */ myInputMixed(),
        /* nonNullableInputWithDefault = */ Present(myInputMixed()),
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
    val query = MyQuery(Absent, 0, Absent, Absent, myInputAbsent(), Absent)
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
            /* nullableInt = */ Absent,
            /* nonNullableInt = */ 1,
            /* nullableMyType = */ Absent,
            /* nonNullableMyType = */
            MyQuery.NonNullableMyType(
                /* nullableInt = */ Absent,
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
            /* nullableInt = */ Present(0),
            /* nonNullableInt = */ 1,
            /* nullableMyType = */
            Present(MyQuery.NullableMyType(
                /* nullableInt = */ Present(2),
                /* nonNullableInt = */ 3,
            )),
            /* nonNullableMyType = */
            MyQuery.NonNullableMyType(
                /* nullableInt = */ Absent,
                /* nonNullableInt = */ 4,
            ),
        ),
        result.dataAssertNoErrors, message = null
    )
  }


  private fun myInputPresent() = MyInput(
      /* nullableInt = */ Present(Present(3)),
      /* nonNullableInt = */ 4,
      /* nonNullableIntWithDefault = */ Present(5),
  )

  private fun myInputAbsent() = MyInput(
      /* nullableInt = */ Absent,
      /* nonNullableInt = */ 4,
      /* nonNullableIntWithDefault = */ Absent,
  )

  private fun myInputMixed() = MyInput(
      /* nullableInt = */ Present(Absent),
      /* nonNullableInt = */ 4,
      /* nonNullableIntWithDefault = */ Present(5),
  )

}
