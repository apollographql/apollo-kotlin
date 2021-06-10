package com.apollographql.apollo3.kmpsample.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.kmpsample.GithubRepositoriesQuery
import com.apollographql.apollo3.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo3.kmpsample.GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.Companion.asCommit
import com.apollographql.apollo3.kmpsample.GithubRepositoryDetailQuery
import com.apollographql.apollo3.kmpsample.GithubRepositoryDetailQuery.Data.Viewer.Repository.Companion.repositoryDetail
import com.apollographql.apollo3.kmpsample.fragment.RepositoryDetail
import com.apollographql.apollo3.kmpsample.fragment.RepositoryFragment
import com.apollographql.apollo3.kmpsample.type.OrderDirection
import com.apollographql.apollo3.kmpsample.type.PullRequestState
import com.apollographql.apollo3.kmpsample.type.RepositoryOrderField
import com.apollographql.apollo3.network.http.HttpResponseInfo
import com.apollographql.apollo3.network.http.HttpNetworkTransport

/**
 * An implementation of a [GitHubDataSource] that shows how we can use coroutines to make our apollo requests.
 */
class ApolloCoroutinesRepository {
  private val apolloClient = ApolloClient(
      networkTransport = HttpNetworkTransport(
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
    val response = apolloClient.query(ApolloRequest(repositoriesQuery))
    println("Http response: " + response.executionContext[HttpResponseInfo])
    return response.data?.viewer?.repositories?.nodes?.mapNotNull { it }.orEmpty()
  }

  suspend fun fetchRepositoryDetail(repositoryName: String): RepositoryDetail? {
    val repositoryDetailQuery = GithubRepositoryDetailQuery(
        name = repositoryName,
        pullRequestStates = listOf(PullRequestState.OPEN)
    )
    val response = apolloClient.query(ApolloRequest(repositoryDetailQuery))
    return response.data?.viewer?.repository?.repositoryDetail()
  }

  suspend fun fetchCommits(repositoryName: String): List<GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.CommitTarget.History.Edge?> {
    val response = apolloClient.query(ApolloRequest(GithubRepositoryCommitsQuery(repositoryName)))
    val headCommit = response.data?.viewer?.repository?.ref?.target?.asCommit()
    return headCommit?.history?.edges.orEmpty()
  }

  companion object {
    private const val GITHUB_KEY = "change me"
  }
}
