package com.apollographql.apollo.api

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
  fun wrapData(data: D): T

  /**
   * Returns GraphQL operation name.
   *
   * @return [OperationName] operation name
   */
  fun name(): OperationName

  /**
   * Returns a unique identifier for this operation.
   *
   * @return operation identifier.
   */
  fun operationId(): String

  /**
   * Abstraction for data returned by the server in response to this operation.
   */
  interface Data {

    /**
     * Returns marshaller to serialize operation data
     *
     * @return [ResponseFieldMarshaller] to serialize operation data
     */
    fun marshaller(): ResponseFieldMarshaller
  }

  /**
   * Abstraction for the variables which are a part of the GraphQL operation. For example, for the following GraphQL
   * operation, Variables represents values for GraphQL '$type' and '$limit' variables:
   * <pre>`query FeedQuery($type: FeedType!, $limit: Int!) {
   * feedEntries: feed(type: $type, limit: $limit) {
   * id
   * repository {
   * ...RepositoryFragment
   * }
   * postedBy {
   * login
   * }
   * }
   * }
  ` *
  </pre> *
   */
  open class Variables {

    open fun valueMap(): Map<String, Any> {
      return emptyMap()
    }

    fun marshaller(): InputFieldMarshaller {
      return object : InputFieldMarshaller {
        override fun marshal(writer: InputFieldWriter) {
        }
      }
    }
  }

  companion object {

    @JvmField
    val EMPTY_VARIABLES = Variables()
  }
}
