package com.apollographql.apollo.api

import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.InputFieldWriter
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.Throws
import com.apollographql.apollo.api.internal.json.InputFieldJsonWriter
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.use
import okio.Buffer
import okio.IOException
import kotlin.jvm.JvmField

/**
 * Represents a GraphQL operation (mutation or query).
 */
interface Operation<D : Operation.Data> {
  /**
   * Returns the raw GraphQL operation String.
   */
  fun queryDocument(): String

  /**
   * Returns the variables associated with this GraphQL operation.
   */
  fun variables(): Variables

  /**
   * Returns an Adapter that maps the server response data to/from generated model class [D].
   */
  fun adapter(): ResponseAdapter<D>

  /**
   * Returns the array of root fields
   */
  fun responseFields(): Array<ResponseField>

  /**
   * Returns GraphQL operation name
   */
  fun name(): String

  /**
   * Returns a unique identifier for this operation.
   */
  fun operationId(): String

  /**
   * Marker interface for generated models built from data returned by the server in response to this operation.
   */
  interface Data

  /**
   * Abstraction for the variables which are a part of the GraphQL operation.
   * For example, for the following GraphQL operation, Variables represents values for GraphQL '$type' and '$limit' variables:
   *
   * ```
   *  query FeedQuery($type: FeedType!, $limit: Int!) {
   *    feedEntries: feed(type: $type, limit: $limit) {
   *      id
   *      repository {
   *        ...RepositoryFragment
   *      }
   *      postedBy {
   *        login
   *      }
   *    }
   * }
   * ```
   */
  open class Variables {

    open fun valueMap(): Map<String, Any?> {
      return emptyMap()
    }

    open fun marshaller(): InputFieldMarshaller {
      return object : InputFieldMarshaller {
        override fun marshal(writer: InputFieldWriter) {
          // noop
        }
      }
    }

    /**
     * Serializes variables as JSON string to be sent to the GraphQL server.
     */
    @Throws(IOException::class)
    fun marshal(): String {
      return marshal(CustomScalarAdapters.DEFAULT)
    }

    /**
     * Serializes variables with provided scalarTypeAdapters [customScalarAdapters] as JSON string to be sent to the GraphQL server.
     */
    @Throws(IOException::class)
    fun marshal(customScalarAdapters: CustomScalarAdapters): String {
      return Buffer().apply {
        JsonWriter.of(this).use { jsonWriter ->
          jsonWriter.serializeNulls = true
          jsonWriter.beginObject()
          marshaller().marshal(InputFieldJsonWriter(jsonWriter, customScalarAdapters))
          jsonWriter.endObject()
        }
      }.readUtf8()
    }
  }

  companion object {

    @JvmField
    val EMPTY_VARIABLES = Variables()
  }
}
