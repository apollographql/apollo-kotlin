package test

import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseJsonResponse
import com.apollographql.apollo.api.toApolloResponse
import okio.buffer
import okio.source
import org.junit.Test
import outofbounds.GetAnimalQuery
import java.io.File

class OutOfBoundsTest {
  @Test
  fun checkOutOfBounds() {
    File("src/main/json/response.json").source().buffer().jsonReader().toApolloResponse(operation = GetAnimalQuery()).dataOrThrow()
  }
}