import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.GlobalBuilder
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.example.GetCatIncludeFalseQuery
import com.example.GetCatIncludeTrueQuery
import com.example.GetCatIncludeVariableQuery
import com.example.GetDogSkipFalseQuery
import com.example.GetDogSkipTrueQuery
import com.example.GetDogSkipVariableQuery
import com.example.type.buildCat
import com.example.type.buildDog
import com.example.type.buildQuery
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class IncludeTest {

  private fun <D : Operation.Data> Operation<D>.parseData(data: Map<String, Any?>): ApolloResponse<D> {
    return parseJsonResponse(MapJsonReader(mapOf("data" to data)))
  }

  @Test
  fun includeVariableTrue() = runBlocking {
    val operation = GetCatIncludeVariableQuery(withCat = true)

    val data = GlobalBuilder.buildQuery {
      animal = buildCat {
        meow = "meeoooowwwww"
      }
    }

    val response = operation.parseData(data)

    assertEquals("meeoooowwwww", response.data!!.animal!!.onCat!!.meow)
  }

  @Test
  fun includeVariableFalse() = runBlocking {
    val operation = GetCatIncludeVariableQuery(withCat = false)

    val data = GlobalBuilder.buildQuery {
      animal = buildCat {
        meow = "meeoooowwwww"
      }
    }

    val response = operation.parseData(data)

    assertEquals(null, response.data!!.animal!!.onCat)
  }

  @Test
  fun includeHardcodedTrue() = runBlocking {
    val operation = GetCatIncludeTrueQuery()

    val data = GlobalBuilder.buildQuery {
      animal = buildCat {
        meow = "meeoooowwwww"
      }
    }

    val response = operation.parseData(data)

    assertEquals("meeoooowwwww", response.data!!.animal!!.onCat!!.meow)
  }

  @Test
  fun includeHardcodedFalse() = runBlocking {
    val operation = GetCatIncludeFalseQuery()

    val data = GlobalBuilder.buildQuery {
      animal = buildCat {
        meow = "meeoooowwwww"
      }
    }

    val response = operation.parseData(data)

    assertEquals(null, response.data!!.animal!!.onCat)
  }

  @Test
  fun skipVariableTrue() = runBlocking {
    val operation = GetDogSkipVariableQuery(withoutDog = true)

    val data = GlobalBuilder.buildQuery {
      animal = buildDog {
        barf = "ouaf"
      }
    }

    val response = operation.parseData(data)

    assertEquals(null, response.data!!.animal!!.dogFragment)
  }

  @Test
  fun skipVariableFalse() = runBlocking {
    val operation = GetDogSkipVariableQuery(withoutDog = false)

    val data = GlobalBuilder.buildQuery {
      animal = buildDog {
        barf = "ouaf"
      }
    }

    val response = operation.parseData(data)

    assertEquals("ouaf", response.data!!.animal!!.dogFragment!!.barf)
  }

  @Test
  fun skipHardcodedTrue() = runBlocking {
    val operation = GetDogSkipTrueQuery()

    val data = GlobalBuilder.buildQuery {
      animal = buildDog {
        barf = "ouaf"
      }
    }

    val response = operation.parseData(data)

    assertEquals(null, response.data!!.animal!!.dogFragment)
  }

  @Test
  fun skipHardcodedFalse() = runBlocking {
    val operation = GetDogSkipFalseQuery()

    val data = GlobalBuilder.buildQuery {
      animal = buildDog {
        barf = "ouaf"
      }
    }

    val response = operation.parseData(data)

    assertEquals("ouaf", response.data!!.animal!!.dogFragment!!.barf)
  }
}

