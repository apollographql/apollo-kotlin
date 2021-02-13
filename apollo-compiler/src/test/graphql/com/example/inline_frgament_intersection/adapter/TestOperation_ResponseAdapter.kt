// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.inline_frgament_intersection.adapter

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ListResponseAdapter
import com.apollographql.apollo.api.internal.NullableResponseAdapter
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.booleanResponseAdapter
import com.apollographql.apollo.api.internal.doubleResponseAdapter
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.stringResponseAdapter
import com.apollographql.apollo.exception.UnexpectedNullValue
import com.example.inline_frgament_intersection.TestOperation
import com.example.inline_frgament_intersection.type.Race
import com.example.inline_frgament_intersection.type.Race_ResponseAdapter
import kotlin.Array
import kotlin.Boolean
import kotlin.Double
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class TestOperation_ResponseAdapter(
  customScalarAdapters: CustomScalarAdapters
) : ResponseAdapter<TestOperation.Data> {
  val randomAdapter: ResponseAdapter<TestOperation.Data.Random> = Random(customScalarAdapters)

  override fun fromResponse(reader: JsonReader, __typename: String?): TestOperation.Data {
    var random: TestOperation.Data.Random? = null
    reader.beginObject()
    while(true) {
      when (reader.selectName(RESPONSE_NAMES)) {
        0 -> random = randomAdapter.fromResponse(reader) ?: throw UnexpectedNullValue("random")
        else -> break
      }
    }
    reader.endObject()
    return TestOperation.Data(
      random = random!!
    )
  }

  override fun toResponse(writer: JsonWriter, value: TestOperation.Data) {
    randomAdapter.toResponse(writer, value.random)
  }

  companion object {
    val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField(
        type = ResponseField.Type.NotNull(ResponseField.Type.Named.Object("Anything")),
        responseName = "random",
        fieldName = "random",
        arguments = emptyMap(),
        conditions = emptyList(),
        fieldSets = listOf(
          ResponseField.FieldSet("Human", Random.BeingHumanRandom.RESPONSE_FIELDS),
          ResponseField.FieldSet("Wookie", Random.BeingWookieRandom.RESPONSE_FIELDS),
          ResponseField.FieldSet(null, Random.OtherRandom.RESPONSE_FIELDS),
        ),
      )
    )

    val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
  }

  class Random(
    customScalarAdapters: CustomScalarAdapters
  ) : ResponseAdapter<TestOperation.Data.Random> {
    val beingHumanRandomAdapter: BeingHumanRandom =
        com.example.inline_frgament_intersection.adapter.TestOperation_ResponseAdapter.Random.BeingHumanRandom(customScalarAdapters)

    val beingWookieRandomAdapter: BeingWookieRandom =
        com.example.inline_frgament_intersection.adapter.TestOperation_ResponseAdapter.Random.BeingWookieRandom(customScalarAdapters)

    val otherRandomAdapter: OtherRandom =
        com.example.inline_frgament_intersection.adapter.TestOperation_ResponseAdapter.Random.OtherRandom(customScalarAdapters)

    override fun fromResponse(reader: JsonReader, __typename: String?): TestOperation.Data.Random {
      reader.beginObject()
      check(reader.nextName() == "__typename")
      val typename = reader.nextString()

      return when(typename) {
        "Human" -> beingHumanRandomAdapter.fromResponse(reader, typename)
        "Wookie" -> beingWookieRandomAdapter.fromResponse(reader, typename)
        else -> otherRandomAdapter.fromResponse(reader, typename)
      }
      .also { reader.endObject() }
    }

    override fun toResponse(writer: JsonWriter, value: TestOperation.Data.Random) {
      when(value) {
        is TestOperation.Data.Random.BeingHumanRandom -> beingHumanRandomAdapter.toResponse(writer, value)
        is TestOperation.Data.Random.BeingWookieRandom -> beingWookieRandomAdapter.toResponse(writer, value)
        is TestOperation.Data.Random.OtherRandom -> otherRandomAdapter.toResponse(writer, value)
      }
    }

    class BeingHumanRandom(
      customScalarAdapters: CustomScalarAdapters
    ) : ResponseAdapter<TestOperation.Data.Random.BeingHumanRandom> {
      val __typenameAdapter: ResponseAdapter<String> = stringResponseAdapter

      val nameAdapter: ResponseAdapter<String> = stringResponseAdapter

      val friendsAdapter: ResponseAdapter<List<TestOperation.Data.Random.BeingHumanRandom.Friend>> =
          ListResponseAdapter(Friend(customScalarAdapters))

      val profilePictureUrlAdapter: ResponseAdapter<String?> =
          NullableResponseAdapter(stringResponseAdapter)

      override fun fromResponse(reader: JsonReader, __typename: String?):
          TestOperation.Data.Random.BeingHumanRandom {
        var __typename: String? = __typename
        var name: String? = null
        var friends: List<TestOperation.Data.Random.BeingHumanRandom.Friend>? = null
        var profilePictureUrl: String? = null
        reader.beginObject()
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = __typenameAdapter.fromResponse(reader) ?: throw
                UnexpectedNullValue("__typename")
            1 -> name = nameAdapter.fromResponse(reader) ?: throw UnexpectedNullValue("name")
            2 -> friends = friendsAdapter.fromResponse(reader) ?: throw
                UnexpectedNullValue("friends")
            3 -> profilePictureUrl = profilePictureUrlAdapter.fromResponse(reader)
            else -> break
          }
        }
        reader.endObject()
        return TestOperation.Data.Random.BeingHumanRandom(
          __typename = __typename!!,
          name = name!!,
          friends = friends!!,
          profilePictureUrl = profilePictureUrl
        )
      }

      override fun toResponse(writer: JsonWriter,
          value: TestOperation.Data.Random.BeingHumanRandom) {
        __typenameAdapter.toResponse(writer, value.__typename)
        nameAdapter.toResponse(writer, value.name)
        friendsAdapter.toResponse(writer, value.friends)
        profilePictureUrlAdapter.toResponse(writer, value.profilePictureUrl)
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField(
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
            responseName = "__typename",
            fieldName = "__typename",
            arguments = emptyMap(),
            conditions = emptyList(),
            fieldSets = emptyList(),
          ),
          ResponseField(
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
            responseName = "name",
            fieldName = "name",
            arguments = emptyMap(),
            conditions = emptyList(),
            fieldSets = emptyList(),
          ),
          ResponseField(
            type =
                ResponseField.Type.NotNull(ResponseField.Type.List(ResponseField.Type.NotNull(ResponseField.Type.Named.Object("Being")))),
            responseName = "friends",
            fieldName = "friends",
            arguments = emptyMap(),
            conditions = emptyList(),
            fieldSets = listOf(
              ResponseField.FieldSet("Wookie", Friend.WookieFriend.RESPONSE_FIELDS),
              ResponseField.FieldSet(null, Friend.OtherFriend.RESPONSE_FIELDS),
            ),
          ),
          ResponseField(
            type = ResponseField.Type.Named.Other("String"),
            responseName = "profilePictureUrl",
            fieldName = "profilePictureUrl",
            arguments = emptyMap(),
            conditions = emptyList(),
            fieldSets = emptyList(),
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }

      class Friend(
        customScalarAdapters: CustomScalarAdapters
      ) : ResponseAdapter<TestOperation.Data.Random.BeingHumanRandom.Friend> {
        val wookieFriendAdapter: WookieFriend =
            com.example.inline_frgament_intersection.adapter.TestOperation_ResponseAdapter.Random.BeingHumanRandom.Friend.WookieFriend(customScalarAdapters)

        val otherFriendAdapter: OtherFriend =
            com.example.inline_frgament_intersection.adapter.TestOperation_ResponseAdapter.Random.BeingHumanRandom.Friend.OtherFriend(customScalarAdapters)

        override fun fromResponse(reader: JsonReader, __typename: String?):
            TestOperation.Data.Random.BeingHumanRandom.Friend {
          reader.beginObject()
          check(reader.nextName() == "__typename")
          val typename = reader.nextString()

          return when(typename) {
            "Wookie" -> wookieFriendAdapter.fromResponse(reader, typename)
            else -> otherFriendAdapter.fromResponse(reader, typename)
          }
          .also { reader.endObject() }
        }

        override fun toResponse(writer: JsonWriter,
            value: TestOperation.Data.Random.BeingHumanRandom.Friend) {
          when(value) {
            is TestOperation.Data.Random.BeingHumanRandom.Friend.WookieFriend -> wookieFriendAdapter.toResponse(writer, value)
            is TestOperation.Data.Random.BeingHumanRandom.Friend.OtherFriend -> otherFriendAdapter.toResponse(writer, value)
          }
        }

        class WookieFriend(
          customScalarAdapters: CustomScalarAdapters
        ) : ResponseAdapter<TestOperation.Data.Random.BeingHumanRandom.Friend.WookieFriend> {
          val __typenameAdapter: ResponseAdapter<String> = stringResponseAdapter

          val nameAdapter: ResponseAdapter<String> = stringResponseAdapter

          val isFamousAdapter: ResponseAdapter<Boolean?> =
              NullableResponseAdapter(booleanResponseAdapter)

          val lifeExpectancyAdapter: ResponseAdapter<Double?> =
              NullableResponseAdapter(doubleResponseAdapter)

          val raceAdapter: ResponseAdapter<Race> = Race_ResponseAdapter

          override fun fromResponse(reader: JsonReader, __typename: String?):
              TestOperation.Data.Random.BeingHumanRandom.Friend.WookieFriend {
            var __typename: String? = __typename
            var name: String? = null
            var isFamous: Boolean? = null
            var lifeExpectancy: Double? = null
            var race: Race? = null
            reader.beginObject()
            while(true) {
              when (reader.selectName(RESPONSE_NAMES)) {
                0 -> __typename = __typenameAdapter.fromResponse(reader) ?: throw
                    UnexpectedNullValue("__typename")
                1 -> name = nameAdapter.fromResponse(reader) ?: throw UnexpectedNullValue("name")
                2 -> isFamous = isFamousAdapter.fromResponse(reader)
                3 -> lifeExpectancy = lifeExpectancyAdapter.fromResponse(reader)
                4 -> race = raceAdapter.fromResponse(reader) ?: throw UnexpectedNullValue("race")
                else -> break
              }
            }
            reader.endObject()
            return TestOperation.Data.Random.BeingHumanRandom.Friend.WookieFriend(
              __typename = __typename!!,
              name = name!!,
              isFamous = isFamous,
              lifeExpectancy = lifeExpectancy,
              race = race!!
            )
          }

          override fun toResponse(writer: JsonWriter,
              value: TestOperation.Data.Random.BeingHumanRandom.Friend.WookieFriend) {
            __typenameAdapter.toResponse(writer, value.__typename)
            nameAdapter.toResponse(writer, value.name)
            isFamousAdapter.toResponse(writer, value.isFamous)
            lifeExpectancyAdapter.toResponse(writer, value.lifeExpectancy)
            raceAdapter.toResponse(writer, value.race)
          }

          companion object {
            val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
              ResponseField(
                type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
                responseName = "__typename",
                fieldName = "__typename",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              ),
              ResponseField(
                type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
                responseName = "name",
                fieldName = "name",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              ),
              ResponseField(
                type = ResponseField.Type.Named.Other("Boolean"),
                responseName = "isFamous",
                fieldName = "isFamous",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              ),
              ResponseField(
                type = ResponseField.Type.Named.Other("Float"),
                responseName = "lifeExpectancy",
                fieldName = "lifeExpectancy",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              ),
              ResponseField(
                type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("Race")),
                responseName = "race",
                fieldName = "race",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              )
            )

            val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
          }
        }

        class OtherFriend(
          customScalarAdapters: CustomScalarAdapters
        ) : ResponseAdapter<TestOperation.Data.Random.BeingHumanRandom.Friend.OtherFriend> {
          val __typenameAdapter: ResponseAdapter<String> = stringResponseAdapter

          val nameAdapter: ResponseAdapter<String> = stringResponseAdapter

          val isFamousAdapter: ResponseAdapter<Boolean?> =
              NullableResponseAdapter(booleanResponseAdapter)

          override fun fromResponse(reader: JsonReader, __typename: String?):
              TestOperation.Data.Random.BeingHumanRandom.Friend.OtherFriend {
            var __typename: String? = __typename
            var name: String? = null
            var isFamous: Boolean? = null
            reader.beginObject()
            while(true) {
              when (reader.selectName(RESPONSE_NAMES)) {
                0 -> __typename = __typenameAdapter.fromResponse(reader) ?: throw
                    UnexpectedNullValue("__typename")
                1 -> name = nameAdapter.fromResponse(reader) ?: throw UnexpectedNullValue("name")
                2 -> isFamous = isFamousAdapter.fromResponse(reader)
                else -> break
              }
            }
            reader.endObject()
            return TestOperation.Data.Random.BeingHumanRandom.Friend.OtherFriend(
              __typename = __typename!!,
              name = name!!,
              isFamous = isFamous
            )
          }

          override fun toResponse(writer: JsonWriter,
              value: TestOperation.Data.Random.BeingHumanRandom.Friend.OtherFriend) {
            __typenameAdapter.toResponse(writer, value.__typename)
            nameAdapter.toResponse(writer, value.name)
            isFamousAdapter.toResponse(writer, value.isFamous)
          }

          companion object {
            val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
              ResponseField(
                type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
                responseName = "__typename",
                fieldName = "__typename",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              ),
              ResponseField(
                type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
                responseName = "name",
                fieldName = "name",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              ),
              ResponseField(
                type = ResponseField.Type.Named.Other("Boolean"),
                responseName = "isFamous",
                fieldName = "isFamous",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              )
            )

            val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
          }
        }
      }
    }

    class BeingWookieRandom(
      customScalarAdapters: CustomScalarAdapters
    ) : ResponseAdapter<TestOperation.Data.Random.BeingWookieRandom> {
      val __typenameAdapter: ResponseAdapter<String> = stringResponseAdapter

      val nameAdapter: ResponseAdapter<String> = stringResponseAdapter

      val friendsAdapter: ResponseAdapter<List<TestOperation.Data.Random.BeingWookieRandom.Friend>>
          = ListResponseAdapter(Friend(customScalarAdapters))

      val raceAdapter: ResponseAdapter<Race> = Race_ResponseAdapter

      override fun fromResponse(reader: JsonReader, __typename: String?):
          TestOperation.Data.Random.BeingWookieRandom {
        var __typename: String? = __typename
        var name: String? = null
        var friends: List<TestOperation.Data.Random.BeingWookieRandom.Friend>? = null
        var race: Race? = null
        reader.beginObject()
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = __typenameAdapter.fromResponse(reader) ?: throw
                UnexpectedNullValue("__typename")
            1 -> name = nameAdapter.fromResponse(reader) ?: throw UnexpectedNullValue("name")
            2 -> friends = friendsAdapter.fromResponse(reader) ?: throw
                UnexpectedNullValue("friends")
            3 -> race = raceAdapter.fromResponse(reader) ?: throw UnexpectedNullValue("race")
            else -> break
          }
        }
        reader.endObject()
        return TestOperation.Data.Random.BeingWookieRandom(
          __typename = __typename!!,
          name = name!!,
          friends = friends!!,
          race = race!!
        )
      }

      override fun toResponse(writer: JsonWriter,
          value: TestOperation.Data.Random.BeingWookieRandom) {
        __typenameAdapter.toResponse(writer, value.__typename)
        nameAdapter.toResponse(writer, value.name)
        friendsAdapter.toResponse(writer, value.friends)
        raceAdapter.toResponse(writer, value.race)
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField(
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
            responseName = "__typename",
            fieldName = "__typename",
            arguments = emptyMap(),
            conditions = emptyList(),
            fieldSets = emptyList(),
          ),
          ResponseField(
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
            responseName = "name",
            fieldName = "name",
            arguments = emptyMap(),
            conditions = emptyList(),
            fieldSets = emptyList(),
          ),
          ResponseField(
            type =
                ResponseField.Type.NotNull(ResponseField.Type.List(ResponseField.Type.NotNull(ResponseField.Type.Named.Object("Being")))),
            responseName = "friends",
            fieldName = "friends",
            arguments = emptyMap(),
            conditions = emptyList(),
            fieldSets = listOf(
              ResponseField.FieldSet("Wookie", Friend.WookieFriend.RESPONSE_FIELDS),
              ResponseField.FieldSet(null, Friend.OtherFriend.RESPONSE_FIELDS),
            ),
          ),
          ResponseField(
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("Race")),
            responseName = "race",
            fieldName = "race",
            arguments = emptyMap(),
            conditions = emptyList(),
            fieldSets = emptyList(),
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }

      class Friend(
        customScalarAdapters: CustomScalarAdapters
      ) : ResponseAdapter<TestOperation.Data.Random.BeingWookieRandom.Friend> {
        val wookieFriendAdapter: WookieFriend =
            com.example.inline_frgament_intersection.adapter.TestOperation_ResponseAdapter.Random.BeingWookieRandom.Friend.WookieFriend(customScalarAdapters)

        val otherFriendAdapter: OtherFriend =
            com.example.inline_frgament_intersection.adapter.TestOperation_ResponseAdapter.Random.BeingWookieRandom.Friend.OtherFriend(customScalarAdapters)

        override fun fromResponse(reader: JsonReader, __typename: String?):
            TestOperation.Data.Random.BeingWookieRandom.Friend {
          reader.beginObject()
          check(reader.nextName() == "__typename")
          val typename = reader.nextString()

          return when(typename) {
            "Wookie" -> wookieFriendAdapter.fromResponse(reader, typename)
            else -> otherFriendAdapter.fromResponse(reader, typename)
          }
          .also { reader.endObject() }
        }

        override fun toResponse(writer: JsonWriter,
            value: TestOperation.Data.Random.BeingWookieRandom.Friend) {
          when(value) {
            is TestOperation.Data.Random.BeingWookieRandom.Friend.WookieFriend -> wookieFriendAdapter.toResponse(writer, value)
            is TestOperation.Data.Random.BeingWookieRandom.Friend.OtherFriend -> otherFriendAdapter.toResponse(writer, value)
          }
        }

        class WookieFriend(
          customScalarAdapters: CustomScalarAdapters
        ) : ResponseAdapter<TestOperation.Data.Random.BeingWookieRandom.Friend.WookieFriend> {
          val __typenameAdapter: ResponseAdapter<String> = stringResponseAdapter

          val nameAdapter: ResponseAdapter<String> = stringResponseAdapter

          val lifeExpectancyAdapter: ResponseAdapter<Double?> =
              NullableResponseAdapter(doubleResponseAdapter)

          override fun fromResponse(reader: JsonReader, __typename: String?):
              TestOperation.Data.Random.BeingWookieRandom.Friend.WookieFriend {
            var __typename: String? = __typename
            var name: String? = null
            var lifeExpectancy: Double? = null
            reader.beginObject()
            while(true) {
              when (reader.selectName(RESPONSE_NAMES)) {
                0 -> __typename = __typenameAdapter.fromResponse(reader) ?: throw
                    UnexpectedNullValue("__typename")
                1 -> name = nameAdapter.fromResponse(reader) ?: throw UnexpectedNullValue("name")
                2 -> lifeExpectancy = lifeExpectancyAdapter.fromResponse(reader)
                else -> break
              }
            }
            reader.endObject()
            return TestOperation.Data.Random.BeingWookieRandom.Friend.WookieFriend(
              __typename = __typename!!,
              name = name!!,
              lifeExpectancy = lifeExpectancy
            )
          }

          override fun toResponse(writer: JsonWriter,
              value: TestOperation.Data.Random.BeingWookieRandom.Friend.WookieFriend) {
            __typenameAdapter.toResponse(writer, value.__typename)
            nameAdapter.toResponse(writer, value.name)
            lifeExpectancyAdapter.toResponse(writer, value.lifeExpectancy)
          }

          companion object {
            val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
              ResponseField(
                type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
                responseName = "__typename",
                fieldName = "__typename",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              ),
              ResponseField(
                type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
                responseName = "name",
                fieldName = "name",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              ),
              ResponseField(
                type = ResponseField.Type.Named.Other("Float"),
                responseName = "lifeExpectancy",
                fieldName = "lifeExpectancy",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              )
            )

            val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
          }
        }

        class OtherFriend(
          customScalarAdapters: CustomScalarAdapters
        ) : ResponseAdapter<TestOperation.Data.Random.BeingWookieRandom.Friend.OtherFriend> {
          val __typenameAdapter: ResponseAdapter<String> = stringResponseAdapter

          val nameAdapter: ResponseAdapter<String> = stringResponseAdapter

          val lifeExpectancyAdapter: ResponseAdapter<Double?> =
              NullableResponseAdapter(doubleResponseAdapter)

          override fun fromResponse(reader: JsonReader, __typename: String?):
              TestOperation.Data.Random.BeingWookieRandom.Friend.OtherFriend {
            var __typename: String? = __typename
            var name: String? = null
            var lifeExpectancy: Double? = null
            reader.beginObject()
            while(true) {
              when (reader.selectName(RESPONSE_NAMES)) {
                0 -> __typename = __typenameAdapter.fromResponse(reader) ?: throw
                    UnexpectedNullValue("__typename")
                1 -> name = nameAdapter.fromResponse(reader) ?: throw UnexpectedNullValue("name")
                2 -> lifeExpectancy = lifeExpectancyAdapter.fromResponse(reader)
                else -> break
              }
            }
            reader.endObject()
            return TestOperation.Data.Random.BeingWookieRandom.Friend.OtherFriend(
              __typename = __typename!!,
              name = name!!,
              lifeExpectancy = lifeExpectancy
            )
          }

          override fun toResponse(writer: JsonWriter,
              value: TestOperation.Data.Random.BeingWookieRandom.Friend.OtherFriend) {
            __typenameAdapter.toResponse(writer, value.__typename)
            nameAdapter.toResponse(writer, value.name)
            lifeExpectancyAdapter.toResponse(writer, value.lifeExpectancy)
          }

          companion object {
            val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
              ResponseField(
                type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
                responseName = "__typename",
                fieldName = "__typename",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              ),
              ResponseField(
                type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
                responseName = "name",
                fieldName = "name",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              ),
              ResponseField(
                type = ResponseField.Type.Named.Other("Float"),
                responseName = "lifeExpectancy",
                fieldName = "lifeExpectancy",
                arguments = emptyMap(),
                conditions = emptyList(),
                fieldSets = emptyList(),
              )
            )

            val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
          }
        }
      }
    }

    class OtherRandom(
      customScalarAdapters: CustomScalarAdapters
    ) : ResponseAdapter<TestOperation.Data.Random.OtherRandom> {
      val __typenameAdapter: ResponseAdapter<String> = stringResponseAdapter

      override fun fromResponse(reader: JsonReader, __typename: String?):
          TestOperation.Data.Random.OtherRandom {
        var __typename: String? = __typename
        reader.beginObject()
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = __typenameAdapter.fromResponse(reader) ?: throw
                UnexpectedNullValue("__typename")
            else -> break
          }
        }
        reader.endObject()
        return TestOperation.Data.Random.OtherRandom(
          __typename = __typename!!
        )
      }

      override fun toResponse(writer: JsonWriter, value: TestOperation.Data.Random.OtherRandom) {
        __typenameAdapter.toResponse(writer, value.__typename)
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField(
            type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
            responseName = "__typename",
            fieldName = "__typename",
            arguments = emptyMap(),
            conditions = emptyList(),
            fieldSets = emptyList(),
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }
    }
  }
}
