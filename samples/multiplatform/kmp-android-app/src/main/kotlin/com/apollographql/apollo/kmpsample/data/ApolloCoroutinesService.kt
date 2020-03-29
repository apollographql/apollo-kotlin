package com.apollographql.apollo.kmpsample.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.coroutines.toDeferred
import com.apollographql.apollo.kmpsample.fragment.RepositoryFragment
import com.apollographql.apollo.kmpsample.GithubRepositoriesQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryDetailQuery
import com.apollographql.apollo.kmpsample.fragment.RepositoryDetail
import com.apollographql.apollo.kmpsample.type.OrderDirection
import com.apollographql.apollo.kmpsample.type.PullRequestState
import com.apollographql.apollo.kmpsample.type.RepositoryOrderField
import kotlinx.coroutines.invoke

/**
 * An implementation of a [GitHubDataSource] that shows how we can use coroutines to make our apollo requests.
 */
class ApolloCoroutinesService(private val apolloClient: ApolloClient) {

  suspend fun fetchRepositories(): List<RepositoryFragment> {
    val repositoriesQuery = GithubRepositoriesQuery(
        repositoriesCount = 50,
        orderBy = RepositoryOrderField.UPDATED_AT,
        orderDirection = OrderDirection.DESC
    )

    val response: Response<GithubRepositoriesQuery.Data> = apolloClient.query(repositoriesQuery).toDeferred().await()
    return response.data?.viewer?.repositories?.nodes?.mapNotNull { it?.fragments?.repositoryFragment }.orEmpty()
  }

  suspend fun fetchRepositoryDetail(repositoryName: String): RepositoryDetail? {
    val repositoryDetailQuery = GithubRepositoryDetailQuery(
        name = repositoryName,
        pullRequestStates = listOf(PullRequestState.OPEN)
    )

    val response: Response<GithubRepositoryDetailQuery.Data> = apolloClient.query(repositoryDetailQuery).toDeferred().await()
    return response.data?.viewer?.repository?.fragments?.repositoryDetail
  }

  suspend fun fetchCommits(repositoryName: String): List<GithubRepositoryCommitsQuery.Edge?> {
    val commitsQuery = GithubRepositoryCommitsQuery(repositoryName)

    val response: Response<GithubRepositoryCommitsQuery.Data> = apolloClient.query(commitsQuery).toDeferred().await()
    val headCommit = response.data?.viewer?.repository?.ref?.target?.asCommit
    return headCommit?.history?.edges.orEmpty()
  }
}
