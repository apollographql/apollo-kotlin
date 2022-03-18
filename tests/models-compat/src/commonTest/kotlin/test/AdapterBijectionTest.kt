package test

import codegen.models.HeroAndFriendsWithFragmentsQuery
import codegen.models.fragment.HeroWithFriendsFragment
import codegen.models.fragment.HumanWithIdFragment
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.toJsonString
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class AdapterBijectionTest {
  @Test
  fun namedFragments() = bijection(
      HeroAndFriendsWithFragmentsQuery(),
      HeroAndFriendsWithFragmentsQuery.Data(
          HeroAndFriendsWithFragmentsQuery.Hero(
              __typename = "Droid",
              fragments = HeroAndFriendsWithFragmentsQuery.Hero.Fragments(
                  heroWithFriendsFragment = HeroWithFriendsFragment(
                      id = "2001",
                      name = "R222-D222",
                      friends = listOf(
                          HeroWithFriendsFragment.Friend(
                              __typename = "Human",
                              fragments = HeroWithFriendsFragment.Friend.Fragments(
                                  humanWithIdFragment = HumanWithIdFragment(
                                      id = "1006",
                                      name = "SuperMan"
                                  )
                              )
                          ),
                          HeroWithFriendsFragment.Friend(
                              __typename = "Human",
                              fragments = HeroWithFriendsFragment.Friend.Fragments(
                                  humanWithIdFragment = HumanWithIdFragment(
                                      id = "1004",
                                      name = "Beast"
                                  )
                              )
                          ),
                      )
                  )
              )
          )
      )
  )

  private fun <D : Operation.Data> bijection(operation: Operation<D>, data: D) {
    val json = operation.adapter().toJsonString(value = data)
    val data2 = operation.adapter().fromJson(Buffer().apply { writeUtf8(json) }.jsonReader(), CustomScalarAdapters.Empty)

    assertEquals(data, data2)
  }
}