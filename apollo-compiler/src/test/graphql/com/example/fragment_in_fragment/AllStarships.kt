// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragment_in_fragment

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
import com.example.fragment_in_fragment.fragment.StarshipFragment
import java.io.IOException
import kotlin.Array
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.jvm.Throws
import okio.BufferedSource

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter")
class AllStarships : Query<AllStarships.Data, AllStarships.Data, Operation.Variables> {
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

  data class Node(
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

      operator fun invoke(reader: ResponseReader): Node {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val fragments = Fragments(reader)
        return Node(
          __typename = __typename,
          fragments = fragments
        )
      }
    }

    data class Fragments(
      val starshipFragment: StarshipFragment
    ) {
      fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
        it.writeFragment(RESPONSE_FIELDS[0], starshipFragment.marshaller())
      }

      companion object {
        private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
            ResponseField.forFragment("__typename", "__typename", listOf(
              ResponseField.Condition.typeCondition(arrayOf("Starship"))
            ))
            )

        operator fun invoke(reader: ResponseReader): Fragments {
          val starshipFragment = reader.readFragment<StarshipFragment>(RESPONSE_FIELDS[0]) {
              reader ->
            StarshipFragment(reader)
          }
          return Fragments(
            starshipFragment = starshipFragment
          )
        }
      }
    }
  }

  data class Edge(
    val __typename: String,
    /**
     * The item at the end of the edge
     */
    val node: Node?
  ) {
    fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeString(RESPONSE_FIELDS[0], __typename)
      it.writeObject(RESPONSE_FIELDS[1], node?.marshaller())
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null),
          ResponseField.forObject("node", "node", null, true, null)
          )

      operator fun invoke(reader: ResponseReader): Edge {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val node = reader.readObject<Node>(RESPONSE_FIELDS[1]) { reader ->
          Node(reader)
        }

        return Edge(
          __typename = __typename,
          node = node
        )
      }
    }
  }

  data class AllStarships(
    val __typename: String,
    /**
     * A list of edges.
     */
    val edges: List<Edge?>?
  ) {
    fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeString(RESPONSE_FIELDS[0], __typename)
      it.writeList(RESPONSE_FIELDS[1], edges) { value, listItemWriter ->
        value?.forEach { value ->
          listItemWriter.writeObject(value?.marshaller())
        }
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null),
          ResponseField.forList("edges", "edges", null, true, null)
          )

      operator fun invoke(reader: ResponseReader): AllStarships {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val edges = reader.readList<Edge>(RESPONSE_FIELDS[1]) {
          it.readObject<Edge> { reader ->
            Edge(reader)
          }

        }
        return AllStarships(
          __typename = __typename,
          edges = edges
        )
      }
    }
  }

  data class Data(
    val allStarships: AllStarships?
  ) : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeObject(RESPONSE_FIELDS[0], allStarships?.marshaller())
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forObject("allStarships", "allStarships", mapOf<String, Any>(
            "first" to "7"), true, null)
          )

      operator fun invoke(reader: ResponseReader): Data {
        val allStarships = reader.readObject<AllStarships>(RESPONSE_FIELDS[0]) { reader ->
          AllStarships(reader)
        }

        return Data(
          allStarships = allStarships
        )
      }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "1296a4041eb330b2810e426f9347f76c6df3a969ab7f7e56f250bf9c6a07982e"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query AllStarships {
          |  allStarships(first: 7) {
          |    __typename
          |    edges {
          |      __typename
          |      node {
          |        __typename
          |        ...starshipFragment
          |      }
          |    }
          |  }
          |}
          |fragment starshipFragment on Starship {
          |  __typename
          |  id
          |  name
          |  pilotConnection {
          |    __typename
          |    edges {
          |      __typename
          |      node {
          |        __typename
          |        ...pilotFragment
          |      }
          |    }
          |  }
          |}
          |fragment pilotFragment on Person {
          |  __typename
          |  name
          |  homeworld {
          |    __typename
          |    ...planetFragment
          |  }
          |}
          |fragment planetFragment on Planet {
          |  __typename
          |  name
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: OperationName = OperationName { "AllStarships" }
  }
}
