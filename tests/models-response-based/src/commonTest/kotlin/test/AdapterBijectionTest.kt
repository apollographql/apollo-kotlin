package test

import codegen.models.HeroAndFriendsWithFragmentsQuery
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
          HeroAndFriendsWithFragmentsQuery.Data.Hero(
              __typename = "Droid",
              id = "2001",
              name = "R222-D222",
              friends = listOf(
                  HeroAndFriendsWithFragmentsQuery.Data.Hero.HumanFriend(
                      __typename = "Human",
                      id = "1006",
                      name = "SuperMan"
                  ),
                  HeroAndFriendsWithFragmentsQuery.Data.Hero.HumanFriend(
                      __typename = "Human",
                      id = "1004",
                      name = "Beast"
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
