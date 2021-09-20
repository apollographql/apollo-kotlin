package test.outofbounds

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.LongAdapter
import com.apollographql.apollo3.api.parseJsonData
import com.apollographql.apollo3.api.parseJsonResponse
import outofbounds.MovieListQuery
import org.junit.Test
import outofbounds.type.MovieListFilterInput
import java.io.File

class OutOfBoundsTest {
  @Test
  fun checkOutOfBounds() {
    val data = MovieListQuery(
        listId = 0,
        limit = 100,
        offset = 0,
        isAppendUserData = true,
        filter = MovieListFilterInput("unused")
    ).parseJsonResponse(
        string = File("src/main/json/response.json").readText(),
        customScalarAdapters = CustomScalarAdapters(
            mapOf("Long" to LongAdapter)
        )
    ).dataOrThrow
    println(data)
  }
}