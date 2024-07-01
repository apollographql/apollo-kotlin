package test

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.toJsonString
import com.apollographql.apollo.integration.normalizer.EpisodeHeroWithDatesQuery
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo.integration.normalizer.HeroNameWithEnumsQuery
import com.apollographql.apollo.integration.normalizer.StarshipByIdQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests that test that transforming a model and parsing it back gives the same model.
 * They test 2 scenarios:
 *
 * model -> json -> model
 * model -> map -> records -> model
 */
class AdapterBijectionTest {
  @Test
  fun customScalar1() = bijection(
      EpisodeHeroWithDatesQuery(Optional.Absent),
      EpisodeHeroWithDatesQuery.Data(
          EpisodeHeroWithDatesQuery.Hero(
              "R222-D222",
              "1985-04-16",
              emptyList()
          )
      )
  )

  @Test
  fun customScalar2() = bijection(
      EpisodeHeroWithDatesQuery(Optional.Absent),
      EpisodeHeroWithDatesQuery.Data(
          EpisodeHeroWithDatesQuery.Hero(
              "R22-D22",
              "1986-04-16",
              listOf(
                  "2017-04-16",
                  "2017-05-16",
              )
          )
      )
  )

  @Test
  fun enum1() = bijection(
      HeroNameWithEnumsQuery(),
      HeroNameWithEnumsQuery.Data(
          HeroNameWithEnumsQuery.Hero(
              "R222-D222",
              Episode.JEDI, emptyList<Episode>()
          )
      )
  )

  @Test
  fun enum2() = bijection(
      HeroNameWithEnumsQuery(),
      HeroNameWithEnumsQuery.Data(
          HeroNameWithEnumsQuery.Hero(
              "R22-D22",
              Episode.JEDI,
              listOf(Episode.EMPIRE)
          )
      )
  )

  @Test
  fun objects1() = bijection(
      HeroAndFriendsNamesWithIDsQuery(Episode.JEDI),
      HeroAndFriendsNamesWithIDsQuery.Data(
          HeroAndFriendsNamesWithIDsQuery.Hero(
              "2001",
              "R222-D222",
              null
          )
      )
  )

  @Test
  fun objects2() = bijection(
      HeroAndFriendsNamesWithIDsQuery(Episode.JEDI),
      HeroAndFriendsNamesWithIDsQuery.Data(
          HeroAndFriendsNamesWithIDsQuery.Hero(
              "2001",
              "R222-D222",
              listOf(
                  HeroAndFriendsNamesWithIDsQuery.Friend(
                      "1002",
                      "Han Soloooo"
                  )
              )
          )
      )
  )


  @Test
  fun listOfList() = bijection(
      StarshipByIdQuery("Starship1"),
      StarshipByIdQuery.Data(
          StarshipByIdQuery.Starship(
              "Starship1",
              "SuperRocket",
              listOf(
                  listOf(900.0, 800.0),
                  listOf(700.0, 600.0)
              )
          )
      )
  )

  /**
   * Fixme: add Fragment.fromResponse() and toJson()
   */
//  @Test
//  fun fragmentImplementation() = bijection(
//      HeroWithFriendsFragmentImpl(),
//      HeroWithFriendsFragmentImpl.Data(
//          __typename = "Droid",
//          id = "2001",
//          name = "R222-D222",
//          friends = listOf(
//              HeroWithFriendsFragmentImpl.Data.HumanFriend(
//                  __typename = "Human",
//                  id = "1000",
//                  name = "SuperMan"
//              ),
//              HeroWithFriendsFragmentImpl.Data.HumanFriend(
//                  __typename = "Human",
//                  id = "1002",
//                  name = "Han Solo"
//              ),
//          )
//      )
//  )

  private fun <D : Operation.Data> bijection(operation: Operation<D>, data: D) {
    val customScalarAdapters = CustomScalarAdapters.Empty
    val json = operation.adapter().toJsonString(value = data, customScalarAdapters)
    val data2 = operation.adapter().fromJson(Buffer().apply { writeUtf8(json) }.jsonReader(), customScalarAdapters)

    assertEquals(data, data2)
  }
}
