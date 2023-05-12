import com.apollographql.apollo3.api.unsafeCastOrCast
import jsexport.GetAnimalQuery
import kotlin.test.Test
import kotlin.test.assertEquals


val response = """
          {
            "animal": {
              "__typename": "Cat",
              "name": "Noushka",
              "species": "Maine Coon"
            }
          }          
        """

expect fun data(response: String): GetAnimalQuery.Data

class JsExportTest {
  @Test
  fun test() {
    val data = data(response)

    assertEquals("Maine Coon", data.animal.species, )
    assertEquals("Cat", data.animal.__typename, )
    assertEquals("Noushka", data.animal.unsafeCastOrCast<GetAnimalQuery.Data.CatAnimal>().name, )
    //assertNull(data.animal.unsafeCastOrCast<GetAnimalQuery.Data.OtherAnimal>(), )
  }
}
