// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.arguments_simple

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.ScalarTypeAdapters.Companion.DEFAULT
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.OperationRequestBodyComposer
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.SimpleOperationResponseParser
import com.apollographql.apollo.api.internal.Throws
import com.example.arguments_simple.fragment.HeroDetails
import com.example.arguments_simple.type.Episode
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.jvm.Transient
import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import okio.IOException

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
data class TestQuery(
  val episode: Input<Episode> = Input.absent(),
  val includeName: Boolean,
  val friendsCount: Int,
  val listOfListOfStringArgs: List<List<String?>>
) : Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
  @Transient
  private val variables: Operation.Variables = object : Operation.Variables() {
    override fun valueMap(): Map<String, Any?> = mutableMapOf<String, Any?>().apply {
      if (this@TestQuery.episode.defined) {
        this["episode"] = this@TestQuery.episode.value
      }
      this["IncludeName"] = this@TestQuery.includeName
      this["friendsCount"] = this@TestQuery.friendsCount
      this["listOfListOfStringArgs"] = this@TestQuery.listOfListOfStringArgs
    }

    override fun marshaller(): InputFieldMarshaller = InputFieldMarshaller.invoke { writer ->
      if (this@TestQuery.episode.defined) {
        writer.writeString("episode", this@TestQuery.episode.value?.rawValue)
      }
      writer.writeBoolean("IncludeName", this@TestQuery.includeName)
      writer.writeInt("friendsCount", this@TestQuery.friendsCount)
      writer.writeList("listOfListOfStringArgs") { listItemWriter ->
        this@TestQuery.listOfListOfStringArgs.forEach { value ->
          listItemWriter.writeList { listItemWriter ->
            value.forEach { value ->
              listItemWriter.writeString(value)
            }
          }
        }
      }
    }
  }

  override fun operationId(): String = OPERATION_ID
  override fun queryDocument(): String = QUERY_DOCUMENT
  override fun wrapData(data: Data?): Data? = data
  override fun variables(): Operation.Variables = variables
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
   * A character from the Star Wars universe
   */
  data class Node(
    override val __typename: String = "Character",
    /**
     * The name of the character
     */
    override val name: String?
  ) : HeroDetails.Node {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@Node.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@Node.name)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, true, listOf(
          ResponseField.Condition.booleanCondition("IncludeName", false)
        ))
      )

      operator fun invoke(reader: ResponseReader): Node = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val name = readString(RESPONSE_FIELDS[1])
        Node(
          __typename = __typename,
          name = name
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Node> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * An edge object for a character's friends
   */
  data class Edge(
    override val __typename: String = "FriendsEdge",
    /**
     * The character represented by this friendship edge
     */
    override val node: Node?
  ) : HeroDetails.Edge {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@Edge.__typename)
        writer.writeObject(RESPONSE_FIELDS[1], this@Edge.node?.marshaller())
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forObject("node", "node", null, true, null)
      )

      operator fun invoke(reader: ResponseReader): Edge = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val node = readObject<Node>(RESPONSE_FIELDS[1]) { reader ->
          Node(reader)
        }
        Edge(
          __typename = __typename,
          node = node
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Edge> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * A connection object for a character's friends
   */
  data class FriendsConnection(
    override val __typename: String = "FriendsConnection",
    /**
     * The total number of friends
     */
    override val totalCount: Int?,
    /**
     * The edges for each of the character's friends.
     */
    override val edges: List<Edge?>?
  ) : HeroDetails.FriendsConnection {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@FriendsConnection.__typename)
        writer.writeInt(RESPONSE_FIELDS[1], this@FriendsConnection.totalCount)
        writer.writeList(RESPONSE_FIELDS[2],
            this@FriendsConnection.edges) { value, listItemWriter ->
          value?.forEach { value ->
            listItemWriter.writeObject(value?.marshaller())}
        }
      }
    }

    fun edgesFilterNotNull(): List<Edge>? = edges?.filterNotNull()

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forInt("totalCount", "totalCount", null, true, null),
        ResponseField.forList("edges", "edges", null, true, null)
      )

      operator fun invoke(reader: ResponseReader): FriendsConnection = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val totalCount = readInt(RESPONSE_FIELDS[1])
        val edges = readList<Edge>(RESPONSE_FIELDS[2]) { reader ->
          reader.readObject<Edge> { reader ->
            Edge(reader)
          }
        }
        FriendsConnection(
          __typename = __typename,
          totalCount = totalCount,
          edges = edges
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<FriendsConnection> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * A character from the Star Wars universe
   */
  data class Hero(
    override val __typename: String = "Character",
    /**
     * The name of the character
     */
    val name: String?,
    /**
     * The friends of the character exposed as a connection with edges
     */
    override val friendsConnection: FriendsConnection
  ) : HeroDetails {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@Hero.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@Hero.name)
        writer.writeObject(RESPONSE_FIELDS[2], this@Hero.friendsConnection.marshaller())
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, true, listOf(
          ResponseField.Condition.booleanCondition("IncludeName", false)
        )),
        ResponseField.forObject("friendsConnection", "friendsConnection", mapOf<String, Any>(
          "first" to mapOf<String, Any>(
            "kind" to "Variable",
            "variableName" to "friendsCount")), false, null)
      )

      operator fun invoke(reader: ResponseReader): Hero = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val name = readString(RESPONSE_FIELDS[1])
        val friendsConnection = readObject<FriendsConnection>(RESPONSE_FIELDS[2]) { reader ->
          FriendsConnection(reader)
        }!!
        Hero(
          __typename = __typename,
          name = name,
          friendsConnection = friendsConnection
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Hero> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * A humanoid creature from the Star Wars universe
   */
  data class HeroWithReview(
    val __typename: String = "Human",
    /**
     * What this human calls themselves
     */
    val name: String
  ) {
    fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@HeroWithReview.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@HeroWithReview.name)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, false, null)
      )

      operator fun invoke(reader: ResponseReader): HeroWithReview = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val name = readString(RESPONSE_FIELDS[1])!!
        HeroWithReview(
          __typename = __typename,
          name = name
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<HeroWithReview> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * Data from the response after executing this GraphQL operation
   */
  data class Data(
    val hero: Hero?,
    val heroWithReview: HeroWithReview?
  ) : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeObject(RESPONSE_FIELDS[0], this@Data.hero?.marshaller())
        writer.writeObject(RESPONSE_FIELDS[1], this@Data.heroWithReview?.marshaller())
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forObject("hero", "hero", mapOf<String, Any>(
          "episode" to mapOf<String, Any>(
            "kind" to "Variable",
            "variableName" to "episode"),
          "listOfListOfStringArgs" to mapOf<String, Any>(
            "kind" to "Variable",
            "variableName" to "listOfListOfStringArgs")), true, null),
        ResponseField.forObject("heroWithReview", "heroWithReview", mapOf<String, Any>(
          "episode" to mapOf<String, Any>(
            "kind" to "Variable",
            "variableName" to "episode"),
          "review" to emptyMap<String, Any>()), true, null)
      )

      operator fun invoke(reader: ResponseReader): Data = reader.run {
        val hero = readObject<Hero>(RESPONSE_FIELDS[0]) { reader ->
          Hero(reader)
        }
        val heroWithReview = readObject<HeroWithReview>(RESPONSE_FIELDS[1]) { reader ->
          HeroWithReview(reader)
        }
        Data(
          hero = hero,
          heroWithReview = heroWithReview
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Data> = ResponseFieldMapper { invoke(it) }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "89afe30dd0fa5ddce3d0b743d3adf68e55a48d0c11d10e495c0ec095949e6d04"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery(${'$'}episode: Episode, ${'$'}IncludeName: Boolean!, ${'$'}friendsCount: Int!, ${'$'}listOfListOfStringArgs: [[String]!]!) {
          |  hero(episode: ${'$'}episode, listOfListOfStringArgs: ${'$'}listOfListOfStringArgs) {
          |    __typename
          |    name @include(if: ${'$'}IncludeName)
          |    ...HeroDetails
          |  }
          |  heroWithReview(episode: ${'$'}episode, review: {}) {
          |    __typename
          |    name
          |  }
          |}
          |fragment HeroDetails on Character {
          |  __typename
          |  friendsConnection(first: ${'$'}friendsCount) {
          |    __typename
          |    totalCount
          |    edges {
          |      __typename
          |      node {
          |        __typename
          |        name @include(if: ${'$'}IncludeName)
          |      }
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
