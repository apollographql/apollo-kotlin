import com.apollographql.apollo3.api.json.DynamicJsJsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import js.test.CreateCustomerMutation
import js.test.GetSalesPeopleQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class JsTest {
  @Test
  fun nameAndIdParametersCompile() {
    CreateCustomerMutation(name = "a", id = 42)
  }

  @Test
  fun dynamicJsonReaderCanParseIntoAdapter() {
    val dynamicResponse = JSON.parse("""
          {
            "data": {
              "getSalesPeople": [
                {
                  "__typename": "foo",
                  "favoriteNumbers": [1, 2, 3],
                  "name": "bob"
                }
              ]
            }
          }
        """.trimIndent()) as dynamic
    val query = GetSalesPeopleQuery()
    val jsonReader = DynamicJsJsonReader(dynamicResponse)
    val response = query.parseJsonResponse(jsonReader)
    assertEquals(
        GetSalesPeopleQuery.Data(
            getSalesPeople = listOf(
                GetSalesPeopleQuery.GetSalesPeople(
                    __typename = "foo",
                    favoriteNumbers = listOf(1, 2, 3),
                    name = "bob"
                )
            )
        ),
        response.data
    )
  }
}
