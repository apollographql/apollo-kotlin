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

  suspend fun fetchRepositories(): List<RepositoryFragment> {
    val repositoriesQuery = GithubRepositoriesQuery(
        repositoriesCount = 50,
        orderBy = RepositoryOrderField.UPDATED_AT,
        orderDirection = OrderDirection.DESC
    )
    val response = apolloClient.query(repositoriesQuery).execute().single()
    println("Http response: " + response.executionContext[HttpExecutionContext.Response])
    return response.data?.viewer?.repositories?.nodes?.mapNotNull { it as RepositoryFragment? }.orEmpty()
  }

  suspend fun fetchRepositoryDetail(repositoryName: String): RepositoryDetail? {
    val repositoryDetailQuery = GithubRepositoryDetailQuery(
        name = repositoryName,
        pullRequestStates = listOf(PullRequestState.OPEN)
    )
    val response = apolloClient.query(repositoryDetailQuery).execute().single()
    return response.data?.viewer?.repository
  }

  suspend fun fetchCommits(repositoryName: String): List<GithubRepositoryCommitsQuery.Edge?> {
    val response = apolloClient.query(GithubRepositoryCommitsQuery(repositoryName)).execute().single()
    val headCommit = response.data?.viewer?.repository?.ref?.target as GithubRepositoryCommitsQuery.CommitTarget?
    return headCommit?.history?.edges.orEmpty()
  }

  companion object {
    private const val GITHUB_KEY = "change me"
  }
}
