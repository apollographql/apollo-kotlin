package test

import codegen.models.HeroAndFriendsWithFragmentsQuery
import codegen.models.fragment.HeroWithFriendsFragment
import codegen.models.fragment.HumanWithIdFragment
import codegen.models.type.Types
import com.apollographql.apollo3.adapters.LocalDateAdapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.fromJson
import com.apollographql.apollo3.api.toJson
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class AdapterBijectionTest {
  @Test
  fun namedFragments() = bijection(
      HeroAndFriendsWithFragmentsQuery(),
      HeroAndFriendsWithFragmentsQuery.Data(
          HeroAndFriendsWithFragmentsQuery.Data.Hero(
              __typename = "Droid",
              heroWithFriendsFragment = HeroWithFriendsFragment(
                  id = "2001",
                  name = "R222-D222",
                  friends = listOf(
                      HeroWithFriendsFragment.Friend(
                          __typename = "Human",
                          humanWithIdFragment = HumanWithIdFragment(
                              id = "1006",
                              name = "SuperMan"
                          )
                      ),
                      HeroWithFriendsFragment.Friend(
                          __typename = "Human",
                          humanWithIdFragment = HumanWithIdFragment(
                              id = "1004",
                              name = "Beast"
                          )
                      ),
                  )
              )
          )
      )
  )

  private fun <D : Operation.Data> bijection(operation: Operation<D>, data: D) {
    val responseAdapterCache = CustomScalarAdapters(mapOf(Types.Date.name to LocalDateAdapter))
    val json = operation.adapter().toJson(value = data, customScalarAdapters = responseAdapterCache)
    val data2 = operation.adapter().fromJson(Buffer().apply { writeUtf8(json) }, responseAdapterCache)

    assertEquals(data, data2)
  }
}