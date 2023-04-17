package test

import com.apollographql.apollo3.adapter.KotlinxLocalDateAdapter
import com.apollographql.apollo3.api.ApolloAdapter
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.fromJson
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.toJsonString
import com.apollographql.apollo3.integration.httpcache.type.Date
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroWithDatesQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameWithEnumsQuery
import com.apollographql.apollo3.integration.normalizer.StarshipByIdQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import kotlinx.datetime.LocalDate
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
              LocalDate(1985, 4, 16),
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
              LocalDate(1986, 4, 16),
              listOf(
                  LocalDate(2017, 4, 16),
                  LocalDate(2017, 5, 16),
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
    val scalarAdapters = ScalarAdapters.Builder().add(Date.type, KotlinxLocalDateAdapter).build()
    val json = operation.adapter().toJsonString(value = data, ApolloAdapter.DataSerializeContext(scalarAdapters = scalarAdapters))
    val data2 = operation.adapter().fromJson(Buffer().apply { writeUtf8(json) }.jsonReader(), scalarAdapters)

    assertEquals(data, data2)
  }
}
