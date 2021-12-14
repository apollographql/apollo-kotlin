package test

import app.cash.turbine.test
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.cache.normalized.optimisticUpdates
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import optimistic.GetAnimalQuery
import optimistic.UpdateAnimalNameMutation
import optimistic.UpdateAnimalSpeciesMutation
import optimistic.type.AnimalInput
import org.junit.Test
import kotlin.test.assertEquals

class OptimisticDataTest {
  private fun optimisticNameData(name: String) = UpdateAnimalNameMutation.Data(
      updateAnimal = UpdateAnimalNameMutation.UpdateAnimal(
          success = true,
          animal = UpdateAnimalNameMutation.Animal(
              id = "1",
              __typename = "Cat",
              name = name
          )
      )
  )

  private fun optimisticSpeciesData(species: String) = UpdateAnimalSpeciesMutation.Data(
      updateAnimal = UpdateAnimalSpeciesMutation.UpdateAnimal(
          success = true,
          animal = UpdateAnimalSpeciesMutation.Animal(
              id = "1",
              __typename = "Cat",
              species = species
          )
      )
  )

  @Test
  fun canRevertAnIntermediateData() = runTest {
    val server = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(server.url())
        .normalizedCache(MemoryCacheFactory())
        .build()

    server.enqueue("""
      {
        "data": {
          "animal": {
            "__typename": "Cat",
            "id": "1",
            "name": "Noushka",
            "species": "Cat"
          }
        }
      }
    """.trimIndent())

    apolloClient.query(GetAnimalQuery("1"))
        .watch()
        .map { it.data?.animal }
        .onEach {
          println(it)
        }
        .filterNotNull()
        .test {
          awaitItem().let {
            // t + 0: First item from the initial response
            assertEquals("Noushka", it.name)
            assertEquals("Cat", it.species)
          }

          server.enqueue(MockResponse(statusCode = 500, delayMillis = 400))
          launch {
            val mutation = UpdateAnimalNameMutation(AnimalInput("Irrelevant"))
            try {
              apolloClient.mutation(mutation)
                  .optimisticUpdates(optimisticNameData("Medor"))
                  .execute()
            } catch (e: Exception) {
              // Ignore
            }
          }

          server.enqueue(MockResponse(statusCode = 501, delayMillis = 10_000))
          val job = launch {
            delay(200)
            val mutation = UpdateAnimalSpeciesMutation(AnimalInput("Irrelevant"))
            apolloClient.mutation(mutation)
                .optimisticUpdates(optimisticSpeciesData("Dog"))
                .execute()
          }

          awaitItem().let {
            // t + 0: Optimistic name change
            assertEquals("Medor", it.name)
            assertEquals("Cat", it.species)
          }
          awaitItem().let {
            // t + 200ms: Optimistic species change
            assertEquals("Medor", it.name)
            assertEquals("Dog", it.species)
          }
          awaitItem().let {
            // t + 400ms: Failure, rollback name change
            assertEquals("Noushka", it.name)
            assertEquals("Dog", it.species)
          }

          // No need to wait for the 10s job to finish
          job.cancel()

          cancelAndIgnoreRemainingEvents()
        }
  }
}
