package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.checkFieldNotMissing
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.writeObject
import com.apollographql.apollo3.api.missingField
import com.apollographql.apollo3.mockserver.TextMessage

/**
 * [FooQuery] is a query for tests that doesn't require codegen.
 *
 * Use it to test parts of the runtime without having to use included builds.
 */
@ApolloExperimental
class FooQuery: FooOperation(), Query<FooOperation.Data> {
  companion object {
    val successResponse = "{\"data\": {\"foo\": 42}}"
  }
}

/**
 * [FooSubscription] is a query for tests that doesn't require codegen.
 *
 * Use it to test parts of the runtime without having to use included builds.
 */
@ApolloExperimental
class FooSubscription: FooOperation(), Subscription<FooOperation.Data> {
  companion object {
    fun nextMessage(id: String, foo: Int): TextMessage {
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
      }.let { TextMessage(it) }
    }

    fun completeMessage(id: String): TextMessage {
      return buildJsonString {
        writeObject {
          name("id")
          value(id)
          name("type")
          value("complete")
        }
      }.let { TextMessage(it) }
    }
  }
}

/**
 * Base class for test queries.
 * Note we can't make [FooOperation] extend both [Query] and [Subscription] because that confuses [ApolloClient] when deciding whant NetworkTransport to use.
 */
@ApolloExperimental
abstract class FooOperation: Operation<FooOperation.Data> {
  class Data(val foo: Int): Query.Data, Subscription.Data {
    override fun toString(): String {
      return "Data(foo: $foo)"
    }
  }

  override fun document(): String {
    return "query GetFoo { foo }"
  }

  override fun name(): String {
    return "FooQuery"
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

  override val ignoreErrors: Boolean
    get() = false
}

