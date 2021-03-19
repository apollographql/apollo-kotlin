package com.apollographql.apollo.kmpsample.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.kmpsample.GithubRepositoriesQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryDetailQuery
import com.apollographql.apollo.kmpsample.fragment.RepositoryDetail
import com.apollographql.apollo.kmpsample.fragment.RepositoryFragment
import com.apollographql.apollo.kmpsample.type.OrderDirection
import com.apollographql.apollo.kmpsample.type.PullRequestState
import com.apollographql.apollo.kmpsample.type.RepositoryOrderField
import com.apollographql.apollo.network.http.ApolloHttpNetworkTransport
import com.apollographql.apollo.network.HttpExecutionContext
import com.apollographql.apollo.ApolloException
import com.apollographql.apollo.network.ws.ApolloWebSocketFactory
import com.apollographql.apollo.network.ws.ApolloWebSocketNetworkTransport
import com.apollographql.apollo.rocketreserver.TripsBookedSubscription
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.single

/**
 * An implementation of a [GitHubDataSource] that shows how we can use coroutines to make our apollo requests.
 */
class ApolloCoroutinesRepository {
  private val apolloClient = ApolloClient(
      networkTransport = ApolloHttpNetworkTransport(
          serverUrl = "https://api.github.com/graphql",
          headers = mapOf(
              "Accept" to "application/json",
              "Content-Type" to "application/json",
              "Authorization" to "bearer $GITHUB_KEY"
          )
      )
  )

  suspend fun subscribeToBookedTrips() {
    val apolloClientWebsocketCommunication = ApolloClient(
      networkTransport = ApolloHttpNetworkTransport(
        serverUrl = "https://apollo-fullstack-tutorial.herokuapp.com/graphql",
        headers = emptyMap()
      ),
      subscriptionNetworkTransport = ApolloWebSocketNetworkTransport(
        webSocketFactory = ApolloWebSocketFactory("https://apollo-fullstack-tutorial.herokuapp.com/graphql")
      )
    )
    apolloClientWebsocketCommunication.subscribe(TripsBookedSubscription()).execute()
      .retryWhen { _, attempt ->
        delay(attempt * 1000)
        true
      }.collect {
        println("Trip Id: ${it.data?.tripsBooked}")
      }
  }

  suspend fun fetchRepositories(): List<RepositoryFragment> {
    val repositoriesQuery = GithubRepositoriesQuery(
        repositoriesCount = 50,
        orderBy = RepositoryOrderField.UPDATED_AT,
        orderDirection = OrderDirection.DESC
    )
    val response = apolloClient.query(repositoriesQuery).execute().single()
    return response.data?.viewer?.repositories?.nodes?.mapNotNull { it?.fragments?.repositoryFragment }.orEmpty()
  }

  suspend fun fetchRepositoryDetail(repositoryName: String): RepositoryDetail? {
    val repositoryDetailQuery = GithubRepositoryDetailQuery(
        name = repositoryName,
        pullRequestStates = listOf(PullRequestState.OPEN)
    )
    val response = apolloClient.query(repositoryDetailQuery).execute().single()
    return response.data?.viewer?.repository?.fragments?.repositoryDetail
  }

  suspend fun fetchCommits(repositoryName: String): List<GithubRepositoryCommitsQuery.Edge?> {
    val response = apolloClient.query(GithubRepositoryCommitsQuery(repositoryName)).execute().single()
    val headCommit = response.data?.viewer?.repository?.ref?.target?.asCommit
    return headCommit?.history?.edges.orEmpty()
  }

  companion object {
    private const val GITHUB_KEY = "change me"
  }
}
