// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.union_inline_fragments

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
import com.example.union_inline_fragments.type.CustomType
import com.example.union_inline_fragments.type.Episode
import java.io.IOException
import kotlin.Array
import kotlin.Deprecated
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

  interface SearchSearchResult {
    fun marshaller(): ResponseFieldMarshaller
  }

  interface FriendCharacter {
    fun marshaller(): ResponseFieldMarshaller
  }

  data class Friend(
    val __typename: String,
    /**
     * The movie this character first appears in
     */
    val firstAppearsIn: Episode
  ) {
    fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeString(RESPONSE_FIELDS[0], __typename)
      it.writeString(RESPONSE_FIELDS[1], firstAppearsIn.rawValue)
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null),
          ResponseField.forEnum("firstAppearsIn", "firstAppearsIn", null, false, null)
          )

      operator fun invoke(reader: ResponseReader): Friend {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val firstAppearsIn = Episode.safeValueOf(reader.readString(RESPONSE_FIELDS[1]))
        return Friend(
          __typename = __typename,
          firstAppearsIn = firstAppearsIn
        )
      }
    }
  }

  data class AsHuman(
    val __typename: String,
    /**
     * The home planet of the human, or null if unknown
     */
    val homePlanet: String?,
    /**
     * This human's friends, or an empty list if they have none
     */
    val friends: List<Friend?>?,
    /**
     * The name of the character
     */
    val name: String
  ) : FriendCharacter {
    override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeString(RESPONSE_FIELDS[0], __typename)
      it.writeString(RESPONSE_FIELDS[1], homePlanet)
      it.writeList(RESPONSE_FIELDS[2], friends) { value, listItemWriter ->
        value?.forEach { value ->
          listItemWriter.writeObject(value?.marshaller())
        }
      }
      it.writeString(RESPONSE_FIELDS[3], name)
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null),
          ResponseField.forString("homePlanet", "homePlanet", null, true, null),
          ResponseField.forList("friends", "friends", null, true, null),
          ResponseField.forString("name", "name", null, false, null)
          )

      operator fun invoke(reader: ResponseReader): AsHuman {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val homePlanet = reader.readString(RESPONSE_FIELDS[1])
        val friends = reader.readList<Friend>(RESPONSE_FIELDS[2]) {
          it.readObject<Friend> { reader ->
            Friend(reader)
          }

        }
        val name = reader.readString(RESPONSE_FIELDS[3])
        return AsHuman(
          __typename = __typename,
          homePlanet = homePlanet,
          friends = friends,
          name = name
        )
      }
    }
  }

  data class Friend1(
    val __typename: String,
    /**
     * The ID of the character
     */
    val id: String,
    /**
     * Test deprecated field
     */
    @Deprecated(message = "For test purpose only")
    val deprecated: String
  ) {
    fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeString(RESPONSE_FIELDS[0], __typename)
      it.writeCustom(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField, id)
      it.writeString(RESPONSE_FIELDS[2], deprecated)
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null),
          ResponseField.forCustomType("id", "id", null, false, CustomType.ID, null),
          ResponseField.forString("deprecated", "deprecated", null, false, null)
          )

      operator fun invoke(reader: ResponseReader): Friend1 {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val id = reader.readCustomType<String>(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField)
        val deprecated = reader.readString(RESPONSE_FIELDS[2])
        return Friend1(
          __typename = __typename,
          id = id,
          deprecated = deprecated
        )
      }
    }
  }

  data class AsDroid(
    val __typename: String,
    /**
     * This droid's primary function
     */
    val primaryFunction: String?,
    /**
     * This droid's friends, or an empty list if they have none
     */
    val friends: List<Friend1?>?,
    /**
     * The name of the character
     */
    val name: String
  ) : FriendCharacter {
    override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeString(RESPONSE_FIELDS[0], __typename)
      it.writeString(RESPONSE_FIELDS[1], primaryFunction)
      it.writeList(RESPONSE_FIELDS[2], friends) { value, listItemWriter ->
        value?.forEach { value ->
          listItemWriter.writeObject(value?.marshaller())
        }
      }
      it.writeString(RESPONSE_FIELDS[3], name)
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null),
          ResponseField.forString("primaryFunction", "primaryFunction", null, true, null),
          ResponseField.forList("friends", "friends", null, true, null),
          ResponseField.forString("name", "name", null, false, null)
          )

      operator fun invoke(reader: ResponseReader): AsDroid {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val primaryFunction = reader.readString(RESPONSE_FIELDS[1])
        val friends = reader.readList<Friend1>(RESPONSE_FIELDS[2]) {
          it.readObject<Friend1> { reader ->
            Friend1(reader)
          }

        }
        val name = reader.readString(RESPONSE_FIELDS[3])
        return AsDroid(
          __typename = __typename,
          primaryFunction = primaryFunction,
          friends = friends,
          name = name
        )
      }
    }
  }

  data class Friend2(
    val __typename: String,
    /**
     * The name of the character
     */
    val name: String,
    val asHuman: AsHuman?,
    val asDroid: AsDroid?
  ) {
    fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeString(RESPONSE_FIELDS[0], __typename)
      it.writeString(RESPONSE_FIELDS[1], name)
      it.writeFragment(RESPONSE_FIELDS[2], asHuman?.marshaller())
      it.writeFragment(RESPONSE_FIELDS[3], asDroid?.marshaller())
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null),
          ResponseField.forString("name", "name", null, false, null),
          ResponseField.forFragment("__typename", "__typename", listOf(
            ResponseField.Condition.typeCondition(arrayOf("Human"))
          )),
          ResponseField.forFragment("__typename", "__typename", listOf(
            ResponseField.Condition.typeCondition(arrayOf("Droid"))
          ))
          )

      operator fun invoke(reader: ResponseReader): Friend2 {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val name = reader.readString(RESPONSE_FIELDS[1])
        val asHuman = reader.readFragment<AsHuman>(RESPONSE_FIELDS[2]) { reader ->
          AsHuman(reader)
        }
        val asDroid = reader.readFragment<AsDroid>(RESPONSE_FIELDS[3]) { reader ->
          AsDroid(reader)
        }
        return Friend2(
          __typename = __typename,
          name = name,
          asHuman = asHuman,
          asDroid = asDroid
        )
      }
    }
  }

  data class AsCharacter(
    val __typename: String,
    /**
     * The ID of the character
     */
    val id: String,
    /**
     * The name of the character
     */
    val name: String,
    /**
     * The friends of the character, or an empty list if they have none
     */
    val friends: List<Friend2?>?
  ) : SearchSearchResult {
    override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeString(RESPONSE_FIELDS[0], __typename)
      it.writeCustom(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField, id)
      it.writeString(RESPONSE_FIELDS[2], name)
      it.writeList(RESPONSE_FIELDS[3], friends) { value, listItemWriter ->
        value?.forEach { value ->
          listItemWriter.writeObject(value?.marshaller())
        }
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null),
          ResponseField.forCustomType("id", "id", null, false, CustomType.ID, null),
          ResponseField.forString("name", "name", null, false, null),
          ResponseField.forList("friends", "friends", null, true, null)
          )

      operator fun invoke(reader: ResponseReader): AsCharacter {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val id = reader.readCustomType<String>(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField)
        val name = reader.readString(RESPONSE_FIELDS[2])
        val friends = reader.readList<Friend2>(RESPONSE_FIELDS[3]) {
          it.readObject<Friend2> { reader ->
            Friend2(reader)
          }

        }
        return AsCharacter(
          __typename = __typename,
          id = id,
          name = name,
          friends = friends
        )
      }
    }
  }

  data class AsStarship(
    val __typename: String,
    /**
     * The name of the starship
     */
    val name: String
  ) : SearchSearchResult {
    override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeString(RESPONSE_FIELDS[0], __typename)
      it.writeString(RESPONSE_FIELDS[1], name)
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null),
          ResponseField.forString("name", "name", null, false, null)
          )

      operator fun invoke(reader: ResponseReader): AsStarship {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val name = reader.readString(RESPONSE_FIELDS[1])
        return AsStarship(
          __typename = __typename,
          name = name
        )
      }
    }
  }

  data class Search(
    val __typename: String,
    val asCharacter: AsCharacter?,
    val asStarship: AsStarship?
  ) {
    fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
      it.writeString(RESPONSE_FIELDS[0], __typename)
      it.writeFragment(RESPONSE_FIELDS[1], asCharacter?.marshaller())
      it.writeFragment(RESPONSE_FIELDS[2], asStarship?.marshaller())
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.forString("__typename", "__typename", null, false, null),
          ResponseField.forFragment("__typename", "__typename", listOf(
            ResponseField.Condition.typeCondition(arrayOf("Human", "Droid"))
          )),
          ResponseField.forFragment("__typename", "__typename", listOf(
            ResponseField.Condition.typeCondition(arrayOf("Starship"))
          ))
          )

      operator fun invoke(reader: ResponseReader): Search {
        val __typename = reader.readString(RESPONSE_FIELDS[0])
        val asCharacter = reader.readFragment<AsCharacter>(RESPONSE_FIELDS[1]) { reader ->
          AsCharacter(reader)
        }
        val asStarship = reader.readFragment<AsStarship>(RESPONSE_FIELDS[2]) { reader ->
          AsStarship(reader)
        }
        return Search(
          __typename = __typename,
          asCharacter = asCharacter,
          asStarship = asStarship
        )
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
        "d917122adce28477721dc274dd7fce307cb1b714452af1df8bb26087b8ec33d0"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  search(text: "test") {
          |    __typename
          |    ... on Character {
          |      id
          |      name
          |      friends {
          |        __typename
          |        ... on Character {
          |          name
          |        }
          |        ... on Human {
          |          homePlanet
          |          friends {
          |            __typename
          |            ... on Character {
          |              firstAppearsIn
          |            }
          |          }
          |        }
          |        ... on Droid {
          |          primaryFunction
          |          friends {
          |            __typename
          |            id
          |            deprecated
          |          }
          |        }
          |      }
          |    }
          |    ... on Starship {
          |      name
          |    }
          |  }
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: OperationName = OperationName { "TestQuery" }
  }
}
