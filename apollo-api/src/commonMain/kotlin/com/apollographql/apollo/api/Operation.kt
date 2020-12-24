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
interface Operation<D : Operation.Data, V : Operation.Variables> {
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
   * Returns GraphQL operation name [OperationName].
   */
  fun name(): OperationName

  /**
   * Returns a unique identifier for this operation.
   */
  fun operationId(): String

  /**
   * Parses GraphQL operation raw response from the [source] with provided [customScalarAdapters] and returns result [Response]
   */
  @Throws(IOException::class)
  fun parse(source: BufferedSource, customScalarAdapters: CustomScalarAdapters): Response<D>

  /**
   * Parses GraphQL operation raw response from the [byteString] with provided [customScalarAdapters] and returns result [Response]
   */
  @Throws(IOException::class)
  fun parse(byteString: ByteString, customScalarAdapters: CustomScalarAdapters): Response<D>

  /**
   * Parses GraphQL operation raw response from the [source] and returns result [Response]
   */
  @Throws(IOException::class)
  fun parse(source: BufferedSource): Response<D>

  /**
   * Parses GraphQL operation raw response from the [byteString] and returns result [Response]
   */
  @Throws(IOException::class)
  fun parse(byteString: ByteString): Response<D>

  /**
   * Composes POST JSON-encoded request body to be sent to the GraphQL server.
   *
   * In case when [autoPersistQueries] is set to `true` special `extension` attributes, required by query auto persistence,
   * will be encoded along with regular GraphQL request body. If query was previously persisted on the GraphQL server
   * set [withQueryDocument] to `false` to skip query document be sent in the request.
   *
   * Optional [customScalarAdapters] must be provided in case when this operation defines variables with custom GraphQL scalar type.
   *
   * *Example*:
   * ```
   * {
   *    "query": "query TestQuery($episode: Episode) { hero(episode: $episode) { name } }",
   *    "operationName": "TestQuery",
   *    "variables": { "episode": "JEDI" }
   *    "extensions": {
   *      "persistedQuery": {
   *        "version": 1,
   *        "sha256Hash": "32637895609e6c51a2593f5cfb49244fd79358d327ff670b3e930e024c3db8f6"
   *      }
   *    }
   * }
   * ```
   */
  fun composeRequestBody(
      autoPersistQueries: Boolean,
      withQueryDocument: Boolean,
      customScalarAdapters: CustomScalarAdapters
  ): ByteString

  /**
   * Composes POST JSON-encoded request body with provided [customScalarAdapters] to be sent to the GraphQL server.
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
  fun composeRequestBody(customScalarAdapters: CustomScalarAdapters): ByteString

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
