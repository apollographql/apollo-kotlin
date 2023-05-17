import com.apollographql.apollo3.api.apolloUnsafeCast
import jsexport.GetAnimalQuery
import kotlin.test.Test
import kotlin.test.assertEquals


val response = """
          {
            "animal": {
              "__typename": "Cat",
              "name": "Noushka",
              "species": "Maine Coon"
            },
            "direction": "SOUTH",
            "point": {
              "x": 1,
              "y": 2
            },
            "bookOrLion": {
              "__typename": "Book",
              "title": "The Lion, the Witch and the Wardrobe"
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
    assertEquals("Noushka", data.animal.apolloUnsafeCast<GetAnimalQuery.Data.CatAnimal>().name, )

    assertEquals("SOUTH", data.direction, )

    assertEquals(1, data.point?.x, )
    assertEquals(2, data.point?.y, )
    assertEquals("The Lion, the Witch and the Wardrobe", data.bookOrLion?.apolloUnsafeCast<GetAnimalQuery.Data.BookBookOrLion>()?.title)
  }
}
