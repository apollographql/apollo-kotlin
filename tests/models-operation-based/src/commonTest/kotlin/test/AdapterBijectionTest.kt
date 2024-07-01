package test

import codegen.models.HeroAndFriendsWithFragmentsQuery
import codegen.models.fragment.HeroWithFriendsFragment
import codegen.models.fragment.HumanWithIdFragment
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.toJsonString
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class AdapterBijectionTest {
  @Test
  fun namedFragments() = bijection(
      HeroAndFriendsWithFragmentsQuery(),
      HeroAndFriendsWithFragmentsQuery.Data(
          HeroAndFriendsWithFragmentsQuery.Hero(
              "Droid",
              HeroWithFriendsFragment(
                  "2001",
                  "R222-D222",
                  listOf(
                      HeroWithFriendsFragment.Friend(
                          "Human",
                          HumanWithIdFragment(
                              "1006",
                              "SuperMan"
                          )
                      ),
                      HeroWithFriendsFragment.Friend(
                          "Human",
                          HumanWithIdFragment(
                              "1004",
                              "Beast"
                          )
                      ),
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
