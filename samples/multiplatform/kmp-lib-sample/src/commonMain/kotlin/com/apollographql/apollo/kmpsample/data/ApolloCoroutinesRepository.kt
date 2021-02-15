package com.apollographql.apollo.kmpsample.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.kmpsample.GithubRepositoriesQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.Companion.asCommitTarget
import com.apollographql.apollo.kmpsample.GithubRepositoryDetailQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryDetailQuery.Data.Viewer.Repository.Companion.asRepositoryRepository
import com.apollographql.apollo.kmpsample.fragment.RepositoryDetail
import com.apollographql.apollo.kmpsample.fragment.RepositoryFragment
import com.apollographql.apollo.kmpsample.type.OrderDirection
import com.apollographql.apollo.kmpsample.type.PullRequestState
import com.apollographql.apollo.kmpsample.type.RepositoryOrderField
import com.apollographql.apollo.network.HttpExecutionContext
import com.apollographql.apollo.network.http.ApolloHttpNetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.single

/**
 * An implementation of a [GitHubDataSource] that shows how we can use coroutines to make our apollo requests.
 */
@OptIn(ApolloExperimental::class, ExperimentalCoroutinesApi::class)
class ApolloCoroutinesRepository {
  private val apolloClient = ApolloClient.Builder()
      .networkTransport(
          ApolloHttpNetworkTransport(
              serverUrl = "https://api.github.com/graphql",
              headers = mapOf(
                  "Accept" to "application/json",
                  "Content-Type" to "application/json",
                  "Authorization" to "bearer $GITHUB_KEY"
              )
          )
      ).build()

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
    return response.data?.viewer?.repository?.asRepositoryRepository()
  }

  suspend fun fetchCommits(repositoryName: String): List<GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.CommitTarget.History.Edges?> {
    val response = apolloClient.query(GithubRepositoryCommitsQuery(repositoryName)).execute().single()
    val headCommit = response.data?.viewer?.repository?.ref?.target?.asCommitTarget()
    return headCommit?.history?.edges.orEmpty()
  }

  companion object {
    private const val GITHUB_KEY = "change me"
  }
}
