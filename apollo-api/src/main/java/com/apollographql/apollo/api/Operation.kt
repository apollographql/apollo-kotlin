package com.apollographql.apollo.api

import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.json.InputFieldJsonWriter
import com.apollographql.apollo.api.internal.json.JsonWriter
import okio.Buffer
import okio.BufferedSource

/**
 * Represents a GraphQL operation (mutation or query).
 */
interface Operation<D : Operation.Data, T, V : Operation.Variables> {
  /**
   * Returns the raw GraphQL operation String.
   */
  fun queryDocument(): String

  /**
   * Returns the variables associated with this GraphQL operation.
   */
  fun variables(): V

  /**
   * Returns a mapper that maps the server response data to generated model class [D].
   */
  fun responseFieldMapper(): ResponseFieldMapper<D>

  /**
   * Wraps the generated response data class [D] with another class. For example, a use case for this would be to
   * wrap the generated response data class in an Optional i.e. Optional.fromNullable(data).
   */
  fun wrapData(data: D?): T?

  /**
   * Returns GraphQL operation name [OperationName].
   */
  fun name(): OperationName

  /**
   * Returns a unique identifier for this operation.
   */
  fun operationId(): String

  /**
   * Parses GraphQL operation raw response from the [source] with provided [scalarTypeAdapters] and returns result [Response]
   */
  fun parse(source: BufferedSource, scalarTypeAdapters: ScalarTypeAdapters): Response<T>

  /**
   * Parses GraphQL operation raw response from the [source] and returns result [Response]
   */
  fun parse(source: BufferedSource): Response<T>

  /**
   * Abstraction for data returned by the server in response to this operation.
   */
  interface Data {

    /**
     * Returns marshaller [ResponseFieldMarshaller] to serialize operation data
     */
    fun marshaller(): ResponseFieldMarshaller
  }

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
      return InputFieldMarshaller { }
    }

    /**
     * Serializes variables as JSON string to be sent to the GraphQL server.
     */
    fun marshal(): String {
      return marshal(ScalarTypeAdapters.DEFAULT)
    }

    /**
     * Serializes variables with provided scalarTypeAdapters [scalarTypeAdapters] as JSON string to be sent to the GraphQL server.
     */
    fun marshal(scalarTypeAdapters: ScalarTypeAdapters): String {
      val buffer = Buffer()
      JsonWriter.of(buffer)
          .apply { serializeNulls = true }
          .beginObject()
          .also { jsonWriter -> marshaller().marshal(InputFieldJsonWriter(jsonWriter, scalarTypeAdapters)) }
          .endObject()
          .close()
      return buffer.readUtf8()
    }
  }

  companion object {

    @JvmField
    val EMPTY_VARIABLES = Variables()
  }
}
