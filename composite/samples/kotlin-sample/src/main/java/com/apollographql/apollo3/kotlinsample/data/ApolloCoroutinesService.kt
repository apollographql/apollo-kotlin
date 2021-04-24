package com.apollographql.apollo3.kotlinsample.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.coroutines.await
import com.apollographql.apollo3.kotlinsample.GithubRepositoriesQuery
import com.apollographql.apollo3.kotlinsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo3.kotlinsample.GithubRepositoryDetailQuery
import com.apollographql.apollo3.kotlinsample.type.OrderDirection
import com.apollographql.apollo3.kotlinsample.type.PullRequestState
import com.apollographql.apollo3.kotlinsample.type.RepositoryOrderField
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * An implementation of a [GitHubDataSource] that shows how we can use coroutines to make our apollo requests.
 */
class ApolloCoroutinesService(
    apolloClient: ApolloClient,
    private val processContext: CoroutineContext = Dispatchers.IO,
    private val resultContext: CoroutineContext = Dispatchers.Main
) : GitHubDataSource(apolloClient) {
  private var job: Job? = null

  override fun fetchRepositories() {
    val repositoriesQuery = GithubRepositoriesQuery(
        repositoriesCount = 50,
        orderBy = RepositoryOrderField.UPDATED_AT,
        orderDirection = OrderDirection.DESC
    )

    job = CoroutineScope(processContext).launch {
      try {
        val response = apolloClient.query(repositoriesQuery).await()
        withContext(resultContext) {
          repositoriesSubject.onNext(mapRepositoriesResponseToRepositories(response))
        }
      } catch (e: Exception) {
        exceptionSubject.onNext(e)
      }
    }
  }

  override fun fetchRepositoryDetail(repositoryName: String) {
    val repositoryDetailQuery = GithubRepositoryDetailQuery(
        name = repositoryName,
        pullRequestStates = listOf(PullRequestState.OPEN)
    )

    job = CoroutineScope(processContext).launch {
      try {
        val response = apolloClient.query(repositoryDetailQuery).await()

        withContext(resultContext) {
          repositoryDetailSubject.onNext(response)
        }
      } catch (e: Exception) {
        exceptionSubject.onNext(e)
      }
    }
  }

  override fun fetchCommits(repositoryName: String) {
    val commitsQuery = GithubRepositoryCommitsQuery(
        name = repositoryName
    )

    job = CoroutineScope(processContext).launch {
      try {
        val response = apolloClient.query(commitsQuery).await()
        val headCommit = response.data?.viewer?.repository?.ref?.target as? GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.CommitTarget

        val commits = headCommit?.history?.edges?.filterNotNull().orEmpty()

        withContext(resultContext) {
          commitsSubject.onNext(commits)
        }
      } catch (e: Exception) {
        exceptionSubject.onNext(e)
      }
    }
  }

  override fun cancelFetching() {
    job?.cancel()
  }
}
