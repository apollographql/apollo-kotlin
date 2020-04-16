package com.apollographql.apollo.kmpsample.data

import com.apollographql.apollo.kmpsample.fragment.RepositoryFragment
import com.apollographql.apollo.kmpsample.GithubRepositoriesQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryDetailQuery
import com.apollographql.apollo.kmpsample.fragment.RepositoryDetail
import com.apollographql.apollo.kmpsample.type.OrderDirection
import com.apollographql.apollo.kmpsample.type.PullRequestState
import com.apollographql.apollo.kmpsample.type.RepositoryOrderField

/**
 * An implementation of a [GitHubDataSource] that shows how we can use coroutines to make our apollo requests.
 */
class ApolloCoroutinesRepository(private val service: ApolloCoroutinesService) {

  suspend fun fetchRepositories(): List<RepositoryFragment> {
    val repositoriesQuery = GithubRepositoriesQuery(
        repositoriesCount = 50,
        orderBy = RepositoryOrderField.UPDATED_AT,
        orderDirection = OrderDirection.DESC
    )
    val response = service.fetchRepositories(repositoriesQuery)
    return response.data?.viewer?.repositories?.nodes?.mapNotNull { it?.fragments?.repositoryFragment }.orEmpty()
  }

  suspend fun fetchRepositoryDetail(repositoryName: String): RepositoryDetail? {
    val repositoryDetailQuery = GithubRepositoryDetailQuery(
        name = repositoryName,
        pullRequestStates = listOf(PullRequestState.OPEN)
    )
    val response = service.fetchRepositoryDetail(repositoryDetailQuery)
    return response.data?.viewer?.repository?.fragments?.repositoryDetail
  }

  suspend fun fetchCommits(repositoryName: String): List<GithubRepositoryCommitsQuery.Edge?> {
    val response = service.fetchCommits(GithubRepositoryCommitsQuery(repositoryName))
    val headCommit = response.data?.viewer?.repository?.ref?.target?.asCommit
    return headCommit?.history?.edges.orEmpty()
  }
}
