// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.union_fragment

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
import com.example.union_fragment.fragment.Character
import com.example.union_fragment.fragment.Starship
import java.io.IOException
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

  data class Search(
    val __typename: String,
    val fragments: Fragments
  ) {
    fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeString(RESPONSE_FIELDS[0], __typename)
      fragments.marshaller().marshal(it)
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null)
          )

      operator fun invoke(reader: ResponseReader): Search {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val fragments = Fragments(reader)
        return Search(
          __typename = __typename,
          fragments = fragments
        )
      }
    }

    data class Fragments(
      val character: Character?,
      val starship: Starship?
    ) {
      fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
        it.writeFragment(character?.marshaller())
        it.writeFragment(starship?.marshaller())
      }

      companion object {
        private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
            ResponseField.forFragment("__typename", "__typename", listOf(
              ResponseField.Condition.typeCondition(arrayOf("Human", "Droid"))
            )),
            ResponseField.forFragment("__typename", "__typename", listOf(
              ResponseField.Condition.typeCondition(arrayOf("Starship"))
            ))
            )

        operator fun invoke(reader: ResponseReader): Fragments {
          val character = reader.readFragment<Character>(RESPONSE_FIELDS[0]) { reader ->
            Character(reader)
          }
          val starship = reader.readFragment<Starship>(RESPONSE_FIELDS[1]) { reader ->
            Starship(reader)
          }
          return Fragments(
            character = character,
            starship = starship
          )
        }
      }
    }
  }

  data class Data(
    val search: List<Search?>?
  ) : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeList(RESPONSE_FIELDS[0], search) { value, listItemWriter ->
        value?.forEach { value ->
          listItemWriter.writeObject(value?.marshaller())
        }
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forList("search", "search", mapOf<String, Any>(
            "text" to "test"), true, null)
          )

      operator fun invoke(reader: ResponseReader): Data {
        val search = reader.readList<Search>(RESPONSE_FIELDS[0]) {
          it.readObject<Search> { reader ->
            Search(reader)
          }

        }
        return Data(
          search = search
        )
      }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "de57eb41c200d48c0f6c508ebf5b4d23b8edd06c6cea371db90ac8160f911b1f"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  search(text: "test") {
          |    __typename
          |    ...Character
          |    ...Starship
          |  }
          |}
          |fragment Character on Character {
          |  __typename
          |  id
          |  name
          |}
          |fragment Starship on Starship {
          |  __typename
          |  name
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: OperationName = OperationName { "TestQuery" }
  }
}
