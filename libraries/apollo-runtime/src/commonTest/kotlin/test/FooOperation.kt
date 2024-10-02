package test

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CompiledField
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.checkFieldNotMissing
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.api.json.buildJsonString
import com.apollographql.apollo.api.json.writeArray
import com.apollographql.apollo.api.json.writeObject
import com.apollographql.apollo.api.missingField

/**
 * [FooQuery] is a query for tests that doesn't require codegen.
 *
 * Use it to test parts of the runtime without having to use included builds.
 */
internal class FooQuery: FooOperation("query"), Query<FooOperation.Data> {
  companion object {
    val successResponse = "{\"data\": {\"foo\": 42}}"
    val errorResponse = "{\"errors\": [{\"message\": \"Oh no! Something went wrong :(\"}]}"
  }
}

/**
 * [FooSubscription] is a query for tests that doesn't require codegen.
 *
 * Use it to test parts of the runtime without having to use included builds.
 */
internal class FooSubscription: FooOperation("subscription"), Subscription<FooOperation.Data> {
  companion object {
    fun nextMessage(id: String, foo: Int): String {
      return buildJsonString {
        writeObject {
          name("id")
          value(id)
          name("type")
          value("next")
          name("payload")
          writeObject {
            name("data")
            writeObject {
              name("foo")
              value(foo)
            }
          }
        }
      }
    }

    fun nextMessage(id: String, errorMessage: String): String {
      return buildJsonString {
        writeObject {
          name("id")
          value(id)
          name("type")
          value("next")
          name("payload")
          writeObject {
            name("errors")
            writeArray {
              writeObject {
                name("message")
                value(errorMessage)
              }
            }
          }
        }
      }
    }

    fun completeMessage(id: String): String {
      return buildJsonString {
        writeObject {
          name("id")
          value(id)
          name("type")
          value("complete")
        }
      }
    }

    fun errorMessage(id: String, message: String): String {
      return buildJsonString {
        writeObject {
          name("id")
          value(id)
          name("type")
          value("error")
          name("payload")
          writeArray {
            writeObject {
              name("message")
              value(message)
            }
          }
        }
      }
    }
  }
}

/**
 * Base class for test queries.
 * Note we can't make [FooOperation] extend both [Query] and [Subscription] because that confuses [ApolloClient] when deciding whant NetworkTransport to use.
 */
internal abstract class FooOperation(private val operationType: String): Operation<FooOperation.Data> {
  class Data(val foo: Int): Query.Data, Subscription.Data {
    override fun toString(): String {
      return "Data(foo: $foo)"
    }
  }

  override fun document(): String {
    return "$operationType FooOperation { foo }"
  }

  override fun name(): String {
    return "FooOperation"
  }

  override fun id(): String {
    return "0"
  }

  override fun adapter(): Adapter<Data> {
    return object :Adapter<Data> {
      override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Data {
        var foo: Int? = null
        reader.beginObject()
        while (reader.hasNext()) {
          when (reader.nextName()) {
            "foo" -> {
              foo = reader.nextInt()
            }
            else -> reader.skipValue()
          }
        }
        reader.endObject()
        checkFieldNotMissing(foo, "foo")
        return Data(foo ?: missingField(reader, "foo"))
      }

      override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Data) {
        writer.name("foo")
        writer.value(value.foo)
      }
    }
  }

  override fun serializeVariables(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, withDefaultValues: Boolean) {
  }

  override fun rootField(): CompiledField {
    TODO("Not yet implemented")
  }
}

