import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.example.GetCatQuery
import com.example.GetDogQuery
import com.example.test.GetCatQuery_TestBuilder
import com.example.test.GetDogQuery_TestBuilder
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class IncludeTest {
  @Test
  fun includeTrue() = runBlocking {
    val operation = GetCatQuery(withCat = true)

    val dataMap = GetCatQuery_TestBuilder.DataBuilder().apply {
      animal = catAnimal {
        meow = "meeoooowwwww"
      }
    }.build()

    val response = operation.parseJsonResponse(
        MapJsonReader(
            mapOf("data" to dataMap)
        )
    )

    assertEquals("meeoooowwwww", response.dataAssertNoErrors.animal!!.onCat!!.meow)
  }

  @Test
  fun includeFalse() = runBlocking {
    val operation = GetCatQuery(withCat = false)

    val dataMap = GetCatQuery_TestBuilder.DataBuilder().apply {
      animal = catAnimal {
        meow = "meeoooowwwww"
      }
    }.build()

    val response = operation.parseJsonResponse(
        MapJsonReader(
            mapOf("data" to dataMap)
        )
    )

    assertEquals(null, response.dataAssertNoErrors.animal!!.onCat)
  }

  @Test
  fun skipTrue() = runBlocking {
    val operation = GetDogQuery(withoutDog = true)

    val dataMap = GetDogQuery_TestBuilder.DataBuilder().apply {
      animal = dogAnimal {
        barf = "ouaf"
      }
    }.build()

    val response = operation.parseJsonResponse(
        MapJsonReader(
            mapOf("data" to dataMap)
        )
    )

    assertEquals(null, response.dataAssertNoErrors.animal!!.dogFragment)
  }

  @Test
  fun skipFalse() = runBlocking {
    val operation = GetDogQuery(withoutDog = false)

    val dataMap = GetDogQuery_TestBuilder.DataBuilder().apply {
      animal = dogAnimal {
        barf = "ouaf"
      }
    }.build()

    val response = operation.parseJsonResponse(
        MapJsonReader(
            mapOf("data" to dataMap)
        )
    )

    assertEquals("ouaf", response.dataAssertNoErrors.animal!!.dogFragment!!.barf)
  }
}

