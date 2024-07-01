
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.GlobalBuilder
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.json.MapJsonReader
import com.apollographql.apollo.api.toApolloResponse
import com.apollographql.apollo.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo.cache.normalized.api.normalize
import com.example.GetCatIncludeFalseQuery
import com.example.GetCatIncludeTrueQuery
import com.example.GetCatIncludeVariableQuery
import com.example.GetCatIncludeVariableWithDefaultQuery
import com.example.GetDogSkipFalseQuery
import com.example.GetDogSkipTrueQuery
import com.example.GetDogSkipVariableQuery
import com.example.SkipFragmentWithDefaultToFalseQuery
import com.example.type.buildCat
import com.example.type.buildDog
import com.example.type.buildQuery
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IncludeTest {

  private fun <D : Operation.Data> Operation<D>.parseData(data: Map<String, Any?>): ApolloResponse<D> {
    return MapJsonReader(mapOf("data" to data)).toApolloResponse(this)
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

    assertEquals("meeoooowwwww", response.dataOrThrow().animal!!.onCat!!.meow)
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

    assertEquals(null, response.dataOrThrow().animal!!.onCat)
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

    assertEquals("meeoooowwwww", response.dataOrThrow().animal!!.onCat!!.meow)
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

    assertEquals(null, response.dataOrThrow().animal!!.onCat)
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

    assertEquals(null, response.dataOrThrow().animal!!.dogFragment)
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

    assertEquals("ouaf", response.dataOrThrow().animal!!.dogFragment!!.barf)
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

    assertEquals(null, response.dataOrThrow().animal!!.dogFragment)
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

    assertEquals("ouaf", response.dataOrThrow().animal!!.dogFragment!!.barf)
  }

  @Test
  fun getCatIncludeVariableWithDefaultQuery(): Unit = runBlocking {
    val operation = GetCatIncludeVariableWithDefaultQuery()

    val data = GetCatIncludeVariableWithDefaultQuery.Data {
      animal = buildCat {
        this["species"] = Optional.Absent
      }
    }

    val normalized = operation.normalize(data, CustomScalarAdapters.Empty, TypePolicyCacheKeyGenerator)
    assertNull((normalized["animal"] as Map<*, *>)["species"])
  }

  @Test
  fun getCatIncludeVariableWithDefaultQuery2() = runBlocking {
    val operation = GetCatIncludeVariableWithDefaultQuery()

    val data = GlobalBuilder.buildQuery {
      animal = buildCat {
      }
    }

    val response = operation.parseData(data)

    assertNull(response.dataOrThrow().animal!!.species)
  }

  @Test
  fun skipFragmentWithDefaultToFalseQuery(): Unit = runBlocking {
    val operation = SkipFragmentWithDefaultToFalseQuery()

    val data = GlobalBuilder.buildQuery {
      animal = buildDog {
        barf = "ouaf"
      }
    }

    val response = operation.parseData(data)

    assertNotNull(response.dataOrThrow().animal!!.dogFragment)
  }

  @Test
  fun skipFragmentWithDefaultToFalseQuery2(): Unit = runBlocking {
    val operation = SkipFragmentWithDefaultToFalseQuery()

    val data = SkipFragmentWithDefaultToFalseQuery.Data {
      animal = buildDog {
        barf = "ouaf"
      }
    }

    val normalized = operation.normalize(data, CustomScalarAdapters.Empty, TypePolicyCacheKeyGenerator)
    assertNull((normalized["animal"] as Map<*, *>)["barf"])
  }
}
