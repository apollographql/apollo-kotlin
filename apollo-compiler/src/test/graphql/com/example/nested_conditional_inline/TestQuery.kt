// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.nested_conditional_inline

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
import com.example.nested_conditional_inline.type.Episode
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Double
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
  val episode: Input<Episode> = Input.absent()
) : Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
  @Transient
  private val variables: Operation.Variables = object : Operation.Variables() {
    override fun valueMap(): Map<String, Any?> = mutableMapOf<String, Any?>().apply {
      if (this@TestQuery.episode.defined) {
        this["episode"] = this@TestQuery.episode.value
      }
    }

    override fun marshaller(): InputFieldMarshaller = InputFieldMarshaller.invoke { writer ->
      if (this@TestQuery.episode.defined) {
        writer.writeString("episode", this@TestQuery.episode.value?.rawValue)
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
   * A humanoid creature from the Star Wars universe
   */
  data class Human1(
    override val __typename: String = "Human",
    /**
     * What this human calls themselves
     */
    override val name: String,
    /**
     * Height in the preferred unit, default is meters
     */
    val height: Double?
  ) : Friend {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@Human1.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@Human1.name)
        writer.writeDouble(RESPONSE_FIELDS[2], this@Human1.height)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, false, null),
        ResponseField.forDouble("height", "height", mapOf<String, Any>(
          "unit" to "FOOT"), true, null)
      )

      operator fun invoke(reader: ResponseReader): Human1 = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val name = readString(RESPONSE_FIELDS[1])!!
        val height = readDouble(RESPONSE_FIELDS[2])
        Human1(
          __typename = __typename,
          name = name,
          height = height
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Human1> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * A character from the Star Wars universe
   */
  data class Otherfriend(
    override val __typename: String = "Character",
    /**
     * The name of the character
     */
    override val name: String
  ) : Friend {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@Otherfriend.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@Otherfriend.name)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, false, null)
      )

      operator fun invoke(reader: ResponseReader): Otherfriend = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val name = readString(RESPONSE_FIELDS[1])!!
        Otherfriend(
          __typename = __typename,
          name = name
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Otherfriend> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * A character from the Star Wars universe
   */
  interface Friend {
    val __typename: String

    /**
     * The name of the character
     */
    val name: String

    fun marshaller(): ResponseFieldMarshaller

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null)
      )

      operator fun invoke(reader: ResponseReader): Friend {
        val typename = reader.readString(RESPONSE_FIELDS[0])
        return when(typename) {
          "Human" -> Human1(reader)
          else -> Otherfriend(reader)
        }
      }
    }
  }

  /**
   * A humanoid creature from the Star Wars universe
   */
  data class Human(
    override val __typename: String = "Human",
    /**
     * What this human calls themselves
     */
    override val name: String,
    /**
     * This human's friends, or an empty list if they have none
     */
    val friends: List<Friend?>?
  ) : Hero {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@Human.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@Human.name)
        writer.writeList(RESPONSE_FIELDS[2], this@Human.friends) { value, listItemWriter ->
          value?.forEach { value ->
            listItemWriter.writeObject(value?.marshaller())}
        }
      }
    }

    fun friendsFilterNotNull(): List<Friend>? = friends?.filterNotNull()

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, false, null),
        ResponseField.forList("friends", "friends", null, true, null)
      )

      operator fun invoke(reader: ResponseReader): Human = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val name = readString(RESPONSE_FIELDS[1])!!
        val friends = readList<Friend>(RESPONSE_FIELDS[2]) { reader ->
          reader.readObject<Friend> { reader ->
            Friend(reader)
          }
        }
        Human(
          __typename = __typename,
          name = name,
          friends = friends
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Human> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * A humanoid creature from the Star Wars universe
   */
  data class Human2(
    override val __typename: String = "Human",
    /**
     * What this human calls themselves
     */
    override val name: String,
    /**
     * Height in the preferred unit, default is meters
     */
    val height: Double?
  ) : Friend1 {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@Human2.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@Human2.name)
        writer.writeDouble(RESPONSE_FIELDS[2], this@Human2.height)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, false, null),
        ResponseField.forDouble("height", "height", mapOf<String, Any>(
          "unit" to "METER"), true, null)
      )

      operator fun invoke(reader: ResponseReader): Human2 = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val name = readString(RESPONSE_FIELDS[1])!!
        val height = readDouble(RESPONSE_FIELDS[2])
        Human2(
          __typename = __typename,
          name = name,
          height = height
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Human2> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * A character from the Star Wars universe
   */
  data class Otherfriend1(
    override val __typename: String = "Character",
    /**
     * The name of the character
     */
    override val name: String
  ) : Friend1 {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@Otherfriend1.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@Otherfriend1.name)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, false, null)
      )

      operator fun invoke(reader: ResponseReader): Otherfriend1 = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val name = readString(RESPONSE_FIELDS[1])!!
        Otherfriend1(
          __typename = __typename,
          name = name
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Otherfriend1> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * A character from the Star Wars universe
   */
  interface Friend1 {
    val __typename: String

    /**
     * The name of the character
     */
    val name: String

    fun marshaller(): ResponseFieldMarshaller

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null)
      )

      operator fun invoke(reader: ResponseReader): Friend1 {
        val typename = reader.readString(RESPONSE_FIELDS[0])
        return when(typename) {
          "Human" -> Human2(reader)
          else -> Otherfriend1(reader)
        }
      }
    }
  }

  /**
   * An autonomous mechanical character in the Star Wars universe
   */
  data class Droid(
    override val __typename: String = "Droid",
    /**
     * What others call this droid
     */
    override val name: String,
    /**
     * This droid's friends, or an empty list if they have none
     */
    val friends: List<Friend1?>?
  ) : Hero {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@Droid.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@Droid.name)
        writer.writeList(RESPONSE_FIELDS[2], this@Droid.friends) { value, listItemWriter ->
          value?.forEach { value ->
            listItemWriter.writeObject(value?.marshaller())}
        }
      }
    }

    fun friendsFilterNotNull(): List<Friend1>? = friends?.filterNotNull()

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, false, null),
        ResponseField.forList("friends", "friends", null, true, null)
      )

      operator fun invoke(reader: ResponseReader): Droid = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val name = readString(RESPONSE_FIELDS[1])!!
        val friends = readList<Friend1>(RESPONSE_FIELDS[2]) { reader ->
          reader.readObject<Friend1> { reader ->
            Friend1(reader)
          }
        }
        Droid(
          __typename = __typename,
          name = name,
          friends = friends
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Droid> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * A character from the Star Wars universe
   */
  data class Otherhero(
    override val __typename: String = "Character",
    /**
     * The name of the character
     */
    override val name: String
  ) : Hero {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@Otherhero.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@Otherhero.name)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, false, null)
      )

      operator fun invoke(reader: ResponseReader): Otherhero = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val name = readString(RESPONSE_FIELDS[1])!!
        Otherhero(
          __typename = __typename,
          name = name
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Otherhero> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * A character from the Star Wars universe
   */
  interface Hero {
    val __typename: String

    /**
     * The name of the character
     */
    val name: String

    fun marshaller(): ResponseFieldMarshaller

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null)
      )

      operator fun invoke(reader: ResponseReader): Hero {
        val typename = reader.readString(RESPONSE_FIELDS[0])
        return when(typename) {
          "Human" -> Human(reader)
          "Droid" -> Droid(reader)
          else -> Otherhero(reader)
        }
      }
    }
  }

  /**
   * Data from the response after executing this GraphQL operation
   */
  data class Data(
    val hero: Hero?
  ) : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeObject(RESPONSE_FIELDS[0], this@Data.hero?.marshaller())
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forObject("hero", "hero", mapOf<String, Any>(
          "episode" to mapOf<String, Any>(
            "kind" to "Variable",
            "variableName" to "episode")), true, null)
      )

      operator fun invoke(reader: ResponseReader): Data = reader.run {
        val hero = readObject<Hero>(RESPONSE_FIELDS[0]) { reader ->
          Hero(reader)
        }
        Data(
          hero = hero
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Data> = ResponseFieldMapper { invoke(it) }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "a9f066a7d1092096ab154f16f32114a4bd71e959b789f37879249cdf6309ea86"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery(${'$'}episode: Episode) {
          |  hero(episode: ${'$'}episode) {
          |    __typename
          |    name
          |    ... on Human {
          |      friends {
          |        __typename
          |        name
          |        ... on Human {
          |          height(unit: FOOT)
          |        }
          |      }
          |    }
          |    ... on Droid {
          |      friends {
          |        __typename
          |        name
          |        ... on Human {
          |          height(unit: METER)
          |        }
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
