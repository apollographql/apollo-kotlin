package test

import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import okio.buffer
import okio.source
import org.junit.Test
import outofbounds.GetAnimalQuery
import java.io.File

class OutOfBoundsTest {
  @Test
  fun checkOutOfBounds() {
    GetAnimalQuery().parseJsonResponse(File("src/main/json/response.json").source().buffer().jsonReader()).dataAssertNoErrors
  }
}