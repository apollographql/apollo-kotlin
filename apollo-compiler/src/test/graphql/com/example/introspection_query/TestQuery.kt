// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.introspection_query

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.ScalarTypeAdapters.Companion.DEFAULT
import com.apollographql.apollo.api.internal.OperationRequestBodyComposer
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.SimpleOperationResponseParser
import com.apollographql.apollo.api.internal.Throws
import kotlin.Array
import kotlin.Boolean
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import okio.IOException

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class TestQuery : Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
  override fun operationId(): String = OPERATION_ID
  override fun queryDocument(): String = QUERY_DOCUMENT
  override fun wrapData(data: Data?): Data? = data
  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES
  override fun name(): OperationName = OPERATION_NAME
  override fun responseFieldMapper(): ResponseFieldMapper<Data> = ResponseFieldMapper.invoke {
    Data(it)
  }

  @Throws(IOException::class)
  override fun parse(source: BufferedSource, scalarTypeAdapters: ScalarTypeAdapters): Response<Data>
      = SimpleOperationResponseParser.parse(source, this, scalarTypeAdapters)

  @Throws(IOException::class)
  override fun parse(byteString: ByteString, scalarTypeAdapters: ScalarTypeAdapters): Response<Data>
      = parse(Buffer().write(byteString), scalarTypeAdapters)

  @Throws(IOException::class)
  override fun parse(source: BufferedSource): Response<Data> = parse(source, DEFAULT)

  @Throws(IOException::class)
  override fun parse(byteString: ByteString): Response<Data> = parse(byteString, DEFAULT)

  override fun composeRequestBody(scalarTypeAdapters: ScalarTypeAdapters): ByteString =
      OperationRequestBodyComposer.compose(
    operation = this,
    autoPersistQueries = false,
    withQueryDocument = true,
    scalarTypeAdapters = scalarTypeAdapters
  )

  override fun composeRequestBody(): ByteString = OperationRequestBodyComposer.compose(
    operation = this,
    autoPersistQueries = false,
    withQueryDocument = true,
    scalarTypeAdapters = DEFAULT
  )

  override fun composeRequestBody(
    autoPersistQueries: Boolean,
    withQueryDocument: Boolean,
    scalarTypeAdapters: ScalarTypeAdapters
  ): ByteString = OperationRequestBodyComposer.compose(
    operation = this,
    autoPersistQueries = autoPersistQueries,
    withQueryDocument = withQueryDocument,
    scalarTypeAdapters = scalarTypeAdapters
  )

  /**
   * The fundamental unit of any GraphQL Schema is the type. There are many kinds of types in
   * GraphQL as represented by the `__TypeKind` enum.
   *
   * Depending on the kind of a type, certain fields describe information about that type. Scalar
   * types provide no information beyond a name and description, while Enum types provide their values.
   * Object and Interface types provide the fields they describe. Abstract types, Union and Interface,
   * provide the Object types possible at runtime. List and NonNull types compose other types.
   */
  data class QueryType(
    val __typename: String = "__Type",
    val name: String?
  ) {
    fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@QueryType.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@QueryType.name)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, true, null)
      )

      operator fun invoke(reader: ResponseReader, __typename: String? = null): QueryType {
        return reader.run {
          var __typename: String? = __typename
          var name: String? = null
          while(true) {
            when (selectField(RESPONSE_FIELDS)) {
              0 -> __typename = readString(RESPONSE_FIELDS[0])
              1 -> name = readString(RESPONSE_FIELDS[1])
              else -> break
            }
          }
          QueryType(
            __typename = __typename!!,
            name = name
          )
        }
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<QueryType> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * The fundamental unit of any GraphQL Schema is the type. There are many kinds of types in
   * GraphQL as represented by the `__TypeKind` enum.
   *
   * Depending on the kind of a type, certain fields describe information about that type. Scalar
   * types provide no information beyond a name and description, while Enum types provide their values.
   * Object and Interface types provide the fields they describe. Abstract types, Union and Interface,
   * provide the Object types possible at runtime. List and NonNull types compose other types.
   */
  data class Type(
    val __typename: String = "__Type",
    val name: String?
  ) {
    fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@Type.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@Type.name)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, true, null)
      )

      operator fun invoke(reader: ResponseReader, __typename: String? = null): Type {
        return reader.run {
          var __typename: String? = __typename
          var name: String? = null
          while(true) {
            when (selectField(RESPONSE_FIELDS)) {
              0 -> __typename = readString(RESPONSE_FIELDS[0])
              1 -> name = readString(RESPONSE_FIELDS[1])
              else -> break
            }
          }
          Type(
            __typename = __typename!!,
            name = name
          )
        }
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Type> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * A GraphQL Schema defines the capabilities of a GraphQL server. It exposes all available types
   * and directives on the server, as well as the entry points for query, mutation, and subscription
   * operations.
   */
  data class __Schema(
    val __typename: String = "__Schema",
    /**
     * The type that query operations will be rooted at.
     */
    val queryType: QueryType,
    /**
     * A list of all types supported by this server.
     */
    val types: List<Type>
  ) {
    fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@__Schema.__typename)
        writer.writeObject(RESPONSE_FIELDS[1], this@__Schema.queryType.marshaller())
        writer.writeList(RESPONSE_FIELDS[2], this@__Schema.types) { value, listItemWriter ->
          value?.forEach { value ->
            listItemWriter.writeObject(value.marshaller())}
        }
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forObject("queryType", "queryType", null, false, null),
        ResponseField.forList("types", "types", null, false, null)
      )

      operator fun invoke(reader: ResponseReader, __typename: String? = null): __Schema {
        return reader.run {
          var __typename: String? = __typename
          var queryType: QueryType? = null
          var types: List<Type>? = null
          while(true) {
            when (selectField(RESPONSE_FIELDS)) {
              0 -> __typename = readString(RESPONSE_FIELDS[0])
              1 -> queryType = readObject<QueryType>(RESPONSE_FIELDS[1]) { reader ->
                QueryType(reader)
              }
              2 -> types = readList<Type>(RESPONSE_FIELDS[2]) { reader ->
                reader.readObject<Type> { reader ->
                  Type(reader)
                }
              }?.map { it!! }
              else -> break
            }
          }
          __Schema(
            __typename = __typename!!,
            queryType = queryType!!,
            types = types!!
          )
        }
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<__Schema> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * The fundamental unit of any GraphQL Schema is the type. There are many kinds of types in
   * GraphQL as represented by the `__TypeKind` enum.
   *
   * Depending on the kind of a type, certain fields describe information about that type. Scalar
   * types provide no information beyond a name and description, while Enum types provide their values.
   * Object and Interface types provide the fields they describe. Abstract types, Union and Interface,
   * provide the Object types possible at runtime. List and NonNull types compose other types.
   */
  data class __Type(
    val __typename: String = "__Type",
    val name: String?
  ) {
    fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@__Type.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@__Type.name)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, true, null)
      )

      operator fun invoke(reader: ResponseReader, __typename: String? = null): __Type {
        return reader.run {
          var __typename: String? = __typename
          var name: String? = null
          while(true) {
            when (selectField(RESPONSE_FIELDS)) {
              0 -> __typename = readString(RESPONSE_FIELDS[0])
              1 -> name = readString(RESPONSE_FIELDS[1])
              else -> break
            }
          }
          __Type(
            __typename = __typename!!,
            name = name
          )
        }
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<__Type> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * Data from the response after executing this GraphQL operation
   */
  data class Data(
    val __schema: __Schema,
    val __type: __Type?
  ) : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeObject(RESPONSE_FIELDS[0], this@Data.__schema.marshaller())
        writer.writeObject(RESPONSE_FIELDS[1], this@Data.__type?.marshaller())
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forObject("__schema", "__schema", null, false, null),
        ResponseField.forObject("__type", "__type", mapOf<String, Any>(
          "name" to "Vehicle"), true, null)
      )

      operator fun invoke(reader: ResponseReader, __typename: String? = null): Data {
        return reader.run {
          var __schema: __Schema? = null
          var __type: __Type? = null
          while(true) {
            when (selectField(RESPONSE_FIELDS)) {
              0 -> __schema = readObject<__Schema>(RESPONSE_FIELDS[0]) { reader ->
                __Schema(reader)
              }
              1 -> __type = readObject<__Type>(RESPONSE_FIELDS[1]) { reader ->
                __Type(reader)
              }
              else -> break
            }
          }
          Data(
            __schema = __schema!!,
            __type = __type
          )
        }
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Data> = ResponseFieldMapper { invoke(it) }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "08518fde8892d59c699c4d48f384d7199d933a5846e6936d910cb492b8f84684"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  __schema {
          |    __typename
          |    queryType {
          |      __typename
          |      name
          |    }
          |    types {
          |      __typename
          |      name
          |    }
          |  }
          |  __type(name: "Vehicle") {
          |    __typename
          |    name
          |  }
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: OperationName = object : OperationName {
      override fun name(): String = "TestQuery"
    }
  }
}
