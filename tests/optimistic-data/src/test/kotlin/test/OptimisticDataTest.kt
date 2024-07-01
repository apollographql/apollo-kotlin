package test

import app.cash.turbine.test
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.cache.normalized.optimisticUpdates
import com.apollographql.apollo.cache.normalized.watch
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
  fun canRevertAnIntermediateData() = runBlocking {
    val server = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(server.url())
        .normalizedCache(MemoryCacheFactory())
        .build()

    server.enqueueString("""
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
        .filterNotNull()
        .test {
          awaitItem().let {
            // t + 0: First item from the initial response
            assertEquals("Noushka", it.name)
            assertEquals("Cat", it.species)
          }

          server.enqueue(MockResponse.Builder().statusCode(500).delayMillis(400).build())
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

          server.enqueue(MockResponse.Builder().statusCode(501).delayMillis(10_000).build())
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
