// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.custom_scalar_type_warnings

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.api.internal.SimpleOperationResponseParser
import com.apollographql.apollo.internal.QueryDocumentMinifier
import com.apollographql.apollo.response.ScalarTypeAdapters
import com.apollographql.apollo.response.ScalarTypeAdapters.DEFAULT
import com.example.custom_scalar_type_warnings.type.CustomType
import java.io.IOException
import kotlin.Any
import kotlin.Array
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.jvm.Throws
import okio.BufferedSource

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter")
class TestQuery : Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
  override fun operationId(): String = OPERATION_ID
  override fun queryDocument(): String = QUERY_DOCUMENT
  override fun wrapData(data: Data?): Data? = data
  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES
  override fun name(): OperationName = OPERATION_NAME
  override fun responseFieldMapper(): ResponseFieldMapper<Data> = ResponseFieldMapper {
    Data(it)
  }

  @Throws(IOException::class)
  override fun parse(source: BufferedSource, scalarTypeAdapters: ScalarTypeAdapters): Response<Data>
      = SimpleOperationResponseParser.parse(source, this, scalarTypeAdapters)

  @Throws(IOException::class)
  override fun parse(source: BufferedSource): Response<Data> = parse(source, DEFAULT)

  data class Hero(
    val __typename: String = "Character",
    /**
     * Links
     */
    val links: List<Any>
  ) {
    fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller { _writer ->
      _writer.writeString(RESPONSE_FIELDS[0], __typename)
      _writer.writeList(RESPONSE_FIELDS[1], links) { _value, _listItemWriter ->
        _value?.forEach { _value ->
          _listItemWriter.writeCustom(CustomType.URL, _value)
        }
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null),
          ResponseField.forList("links", "links", null, false, null)
          )

      operator fun invoke(reader: ResponseReader): Hero {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val links = reader.readList<Any>(RESPONSE_FIELDS[1]) {
          it.readCustomType<Any>(CustomType.URL)
        }
        return Hero(
          __typename = __typename,
          links = links
        )
      }
    }
  }

  data class Data(
    val hero: Hero?
  ) : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller { _writer ->
      _writer.writeObject(RESPONSE_FIELDS[0], hero?.marshaller())
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forObject("hero", "hero", null, true, null)
          )

      operator fun invoke(reader: ResponseReader): Data {
        val hero = reader.readObject<Hero>(RESPONSE_FIELDS[0]) { reader ->
          Hero(reader)
        }

        return Data(
          hero = hero
        )
      }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "1a019419389595f8e5269db271bc43dae6cf9733959296ceff6a3270faa91c62"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  hero {
          |    __typename
          |    links
          |  }
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: OperationName = OperationName { "TestQuery" }
  }
}
