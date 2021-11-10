package test.outofbounds

import com.apollographql.apollo3.api.parseJsonResponse
import org.junit.Test
import outofbounds.GetAnimalQuery
import java.io.File

class OutOfBoundsTest {
  @Test
  fun checkOutOfBounds() {
    val data = GetAnimalQuery().parseJsonResponse(
        string = File("src/main/json/response.json").readText(),
    ).dataAssertNoErrors
  }
}