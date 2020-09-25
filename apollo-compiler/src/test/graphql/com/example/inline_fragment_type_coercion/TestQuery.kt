// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.inline_fragment_type_coercion

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
   * For testing fragment type coercion
   */
  interface Bar : Foo {
    override val __typename: String

    override val foo: String

    val bar: String

    override fun marshaller(): ResponseFieldMarshaller
  }

  /**
   * For testing fragment type coercion
   */
  data class BarImpl(
    override val __typename: String = "Bar",
    override val foo: String,
    override val bar: String
  ) : Bar, Foo {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@BarImpl.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@BarImpl.foo)
        writer.writeString(RESPONSE_FIELDS[2], this@BarImpl.bar)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("foo", "foo", null, false, null),
        ResponseField.forString("bar", "bar", null, false, null)
      )

      operator fun invoke(reader: ResponseReader): BarImpl = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val foo = readString(RESPONSE_FIELDS[1])!!
        val bar = readString(RESPONSE_FIELDS[2])!!
        BarImpl(
          __typename = __typename,
          foo = foo,
          bar = bar
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<BarImpl> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * For testing fragment type coercion
   */
  data class FooImpl(
    override val __typename: String = "Foo",
    override val foo: String
  ) : Foo {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@FooImpl.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@FooImpl.foo)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("foo", "foo", null, false, null)
      )

      operator fun invoke(reader: ResponseReader): FooImpl = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val foo = readString(RESPONSE_FIELDS[1])!!
        FooImpl(
          __typename = __typename,
          foo = foo
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<FooImpl> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * For testing fragment type coercion
   */
  interface Foo {
    val __typename: String

    val foo: String

    fun marshaller(): ResponseFieldMarshaller

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null)
      )

      operator fun invoke(reader: ResponseReader): Foo {
        val typename = reader.readString(RESPONSE_FIELDS[0])
        return when(typename) {
          "BarObject" -> BarImpl(reader)
          "FooBar" -> BarImpl(reader)
          else -> FooImpl(reader)
        }
      }
    }
  }

  /**
   * Data from the response after executing this GraphQL operation
   */
  data class Data(
    /**
     * For testing fragment type coercion
     */
    val foo: Foo?
  ) : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeObject(RESPONSE_FIELDS[0], this@Data.foo?.marshaller())
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forObject("foo", "foo", null, true, null)
      )

      operator fun invoke(reader: ResponseReader): Data = reader.run {
        val foo = readObject<Foo>(RESPONSE_FIELDS[0]) { reader ->
          Foo(reader)
        }
        Data(
          foo = foo
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Data> = ResponseFieldMapper { invoke(it) }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "65c4fd857f5cbd2283f0783a3b3cefd9ead5abb159f5cc20922b0d8e04286662"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  foo {
          |    __typename
          |    foo
          |    ... on Bar {
          |      bar
          |    }
          |  }
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: OperationName = object : OperationName {
      override fun name(): String = "TestQuery"
    }
  }
}
