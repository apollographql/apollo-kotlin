import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.example.GetCatIncludeFalseQuery
import com.example.GetCatIncludeTrueQuery
import com.example.GetCatIncludeVariableQuery
import com.example.GetDogSkipFalseQuery
import com.example.GetDogSkipTrueQuery
import com.example.GetDogSkipVariableQuery
import com.example.test.GetCatIncludeFalseQuery_TestBuilder
import com.example.test.GetCatIncludeTrueQuery_TestBuilder
import com.example.test.GetCatIncludeVariableQuery_TestBuilder
import com.example.test.GetDogSkipFalseQuery_TestBuilder
import com.example.test.GetDogSkipTrueQuery_TestBuilder
import com.example.test.GetDogSkipVariableQuery_TestBuilder
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class IncludeTest {
  @Test
  fun includeVariableTrue() = runBlocking {
    val operation = GetCatIncludeVariableQuery(withCat = true)

    val dataMap = GetCatIncludeVariableQuery_TestBuilder.DataBuilder().apply {
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
  fun includeVariableFalse() = runBlocking {
    val operation = GetCatIncludeVariableQuery(withCat = false)

    val dataMap = GetCatIncludeVariableQuery_TestBuilder.DataBuilder().apply {
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
  fun includeHardcodedTrue() = runBlocking {
    val operation = GetCatIncludeTrueQuery()

    val dataMap = GetCatIncludeTrueQuery_TestBuilder.DataBuilder().apply {
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
  fun includeHardcodedFalse() = runBlocking {
    val operation = GetCatIncludeFalseQuery()

    val dataMap = GetCatIncludeFalseQuery_TestBuilder.DataBuilder().apply {
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
  fun skipVariableTrue() = runBlocking {
    val operation = GetDogSkipVariableQuery(withoutDog = true)

    val dataMap = GetDogSkipVariableQuery_TestBuilder.DataBuilder().apply {
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
  fun skipVariableFalse() = runBlocking {
    val operation = GetDogSkipVariableQuery(withoutDog = false)

    val dataMap = GetDogSkipVariableQuery_TestBuilder.DataBuilder().apply {
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

  @Test
  fun skipHardcodedTrue() = runBlocking {
    val operation = GetDogSkipTrueQuery()

    val dataMap = GetDogSkipTrueQuery_TestBuilder.DataBuilder().apply {
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
  fun skipHardcodedFalse() = runBlocking {
    val operation = GetDogSkipFalseQuery()

    val dataMap = GetDogSkipFalseQuery_TestBuilder.DataBuilder().apply {
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

