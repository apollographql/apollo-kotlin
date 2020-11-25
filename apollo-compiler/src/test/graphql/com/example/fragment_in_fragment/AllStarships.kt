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
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.ScalarTypeAdapters.Companion.DEFAULT
import com.apollographql.apollo.api.internal.OperationRequestBodyComposer
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.SimpleOperationResponseParser
import com.apollographql.apollo.api.internal.Throws
import com.example.fragment_in_fragment.fragment.PilotFragment
import com.example.fragment_in_fragment.fragment.PlanetFragment
import com.example.fragment_in_fragment.fragment.StarshipFragment
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
class AllStarships : Query<AllStarships.Data, Operation.Variables> {
  override fun operationId(): String = OPERATION_ID

  override fun queryDocument(): String = QUERY_DOCUMENT

  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  override fun name(): OperationName = OPERATION_NAME

  override fun responseFieldMapper(): ResponseFieldMapper<Data> {
    return ResponseFieldMapper { reader ->
      AllStarships_ResponseAdapter.fromResponse(reader)
    }
  }

  @Throws(IOException::class)
  override fun parse(source: BufferedSource, scalarTypeAdapters: ScalarTypeAdapters):
      Response<Data> {
    return SimpleOperationResponseParser.parse(source, this, scalarTypeAdapters)
  }

  @Throws(IOException::class)
  override fun parse(byteString: ByteString, scalarTypeAdapters: ScalarTypeAdapters):
      Response<Data> {
    return parse(Buffer().write(byteString), scalarTypeAdapters)
  }

  @Throws(IOException::class)
  override fun parse(source: BufferedSource): Response<Data> {
    return parse(source, DEFAULT)
  }

  @Throws(IOException::class)
  override fun parse(byteString: ByteString): Response<Data> {
    return parse(byteString, DEFAULT)
  }

  override fun composeRequestBody(scalarTypeAdapters: ScalarTypeAdapters): ByteString {
    return OperationRequestBodyComposer.compose(
      operation = this,
      autoPersistQueries = false,
      withQueryDocument = true,
      scalarTypeAdapters = scalarTypeAdapters
    )
  }

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
   * A large mass, planet or planetoid in the Star Wars Universe, at the time of
   * 0 ABY.
   */
  data class Homeworld(
    override val __typename: String = "Planet",
    /**
     * The name of this planet.
     */
    override val name: String?
  ) : PlanetFragment, PilotFragment.Homeworld {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller { writer ->
        AllStarships_ResponseAdapter.Homeworld_ResponseAdapter.toResponse(writer, this)
      }
    }
  }

  /**
   * An individual person or character within the Star Wars universe.
   */
  data class Node1(
    override val __typename: String = "Person",
    /**
     * The name of this person.
     */
    override val name: String?,
    /**
     * A planet that this person was born on or inhabits.
     */
    override val homeworld: Homeworld?
  ) : PilotFragment, StarshipFragment.Node {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller { writer ->
        AllStarships_ResponseAdapter.Node1_ResponseAdapter.toResponse(writer, this)
      }
    }
  }

  /**
   * An edge in a connection.
   */
  data class Edge1(
    /**
     * The item at the end of the edge
     */
    override val node: Node1?
  ) : StarshipFragment.Edge {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller { writer ->
        AllStarships_ResponseAdapter.Edge1_ResponseAdapter.toResponse(writer, this)
      }
    }
  }

  /**
   * A connection to a list of items.
   */
  data class PilotConnection(
    /**
     * A list of edges.
     */
    override val edges: List<Edge1?>?
  ) : StarshipFragment.PilotConnection {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller { writer ->
        AllStarships_ResponseAdapter.PilotConnection_ResponseAdapter.toResponse(writer, this)
      }
    }

    fun edgesFilterNotNull(): List<Edge1>? = edges?.filterNotNull()
  }

  /**
   * A single transport craft that has hyperdrive capability.
   */
  data class Node(
    override val __typename: String = "Starship",
    /**
     * The ID of an object
     */
    override val id: String,
    /**
     * The name of this starship. The common name, such as "Death Star".
     */
    override val name: String?,
    override val pilotConnection: PilotConnection?
  ) : StarshipFragment {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller { writer ->
        AllStarships_ResponseAdapter.Node_ResponseAdapter.toResponse(writer, this)
      }
    }
  }

  /**
   * An edge in a connection.
   */
  data class Edge(
    /**
     * The item at the end of the edge
     */
    val node: Node?
  ) {
    fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller { writer ->
        AllStarships_ResponseAdapter.Edge_ResponseAdapter.toResponse(writer, this)
      }
    }
  }

  /**
   * A connection to a list of items.
   */
  data class AllStarships1(
    /**
     * A list of edges.
     */
    val edges: List<Edge?>?
  ) {
    fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller { writer ->
        AllStarships_ResponseAdapter.AllStarships1_ResponseAdapter.toResponse(writer, this)
      }
    }

    fun edgesFilterNotNull(): List<Edge>? = edges?.filterNotNull()
  }

  /**
   * Data from the response after executing this GraphQL operation
   */
  data class Data(
    val allStarships: AllStarships1?
  ) : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller { writer ->
        AllStarships_ResponseAdapter.toResponse(writer, this)
      }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "3d7fa8ee44e23f44c0605c01b0db776614b2dd3d78541751c50cb04d9ba4c4cc"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query AllStarships {
          |  allStarships(first: 7) {
          |    edges {
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
          |    edges {
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

    val OPERATION_NAME: OperationName = object : OperationName {
      override fun name(): String {
        return "AllStarships"
      }
    }
  }
}
