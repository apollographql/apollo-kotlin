package com.apollographql.apollo.api

import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.InputFieldWriter
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.Throws
import com.apollographql.apollo.api.internal.json.InputFieldJsonWriter
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.use
import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import okio.IOException
import kotlin.jvm.JvmField

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
  @Throws(IOException::class)
  fun parse(source: BufferedSource, scalarTypeAdapters: ScalarTypeAdapters): Response<T>

  /**
   * Parses GraphQL operation raw response from the [byteString] with provided [scalarTypeAdapters] and returns result [Response]
   */
  @Throws(IOException::class)
  fun parse(byteString: ByteString, scalarTypeAdapters: ScalarTypeAdapters): Response<T>

  /**
   * Parses GraphQL operation raw response from the [source] and returns result [Response]
   */
  @Throws(IOException::class)
  fun parse(source: BufferedSource): Response<T>

  /**
   * Parses GraphQL operation raw response from the [byteString] and returns result [Response]
   */
  @Throws(IOException::class)
  fun parse(byteString: ByteString): Response<T>

  /**
   * Composes POST JSON-encoded request body with provided [scalarTypeAdapters] to be sent to the GraphQL server.
   *
   * *Example*:
   * ```
   * {
   *    "query": "query TestQuery($episode: Episode) { hero(episode: $episode) { name } }",
   *    "operationName": "TestQuery",
   *    "variables": { "episode": "JEDI" }
   * }
   * ```
   */
  fun composeRequestBody(scalarTypeAdapters: ScalarTypeAdapters): ByteString

  /**
   * Composes POST JSON-encoded request body to be sent to the GraphQL server.
   *
   * *Example*:
   * ```
   * {
   *    "query": "query TestQuery($episode: Episode) { hero(episode: $episode) { name } }",
   *    "operationName": "TestQuery",
   *    "variables": { "episode": "JEDI" }
   * }
   * ```
   */
  fun composeRequestBody(): ByteString

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
      return marshal(ScalarTypeAdapters.DEFAULT)
    }

    /**
     * Serializes variables with provided scalarTypeAdapters [scalarTypeAdapters] as JSON string to be sent to the GraphQL server.
     */
    @Throws(IOException::class)
    fun marshal(scalarTypeAdapters: ScalarTypeAdapters): String {
      return Buffer().apply {
        JsonWriter.of(this).use { jsonWriter ->
          jsonWriter.serializeNulls = true
          jsonWriter.beginObject()
          marshaller().marshal(InputFieldJsonWriter(jsonWriter, scalarTypeAdapters))
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
