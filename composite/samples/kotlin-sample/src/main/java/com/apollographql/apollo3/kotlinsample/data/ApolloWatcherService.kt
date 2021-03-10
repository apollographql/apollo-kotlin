package com.apollographql.apollo3.kotlinsample.data

import android.util.Log
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.fetcher.ApolloResponseFetchers
import com.apollographql.apollo3.kotlinsample.GithubRepositoriesQuery
import com.apollographql.apollo3.kotlinsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo3.kotlinsample.GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.Companion.asCommitTarget
import com.apollographql.apollo3.kotlinsample.GithubRepositoryDetailQuery
import com.apollographql.apollo3.kotlinsample.type.OrderDirection
import com.apollographql.apollo3.kotlinsample.type.PullRequestState
import com.apollographql.apollo3.kotlinsample.type.RepositoryOrderField

/**
 * An implementation of a [GitHubDataSource] that shows how to fetch data using callbacks.
 */
class ApolloWatcherService(apolloClient: ApolloClient) : GitHubDataSource(apolloClient) {
  override fun fetchRepositories() {
    val repositoriesQuery = GithubRepositoriesQuery(
        repositoriesCount = 50,
        orderBy = RepositoryOrderField.UPDATED_AT,
        orderDirection = OrderDirection.DESC
    )

    val callback = createCallback<GithubRepositoriesQuery.Data> {
      repositoriesSubject.onNext(mapRepositoriesResponseToRepositories(it))
    }

    apolloClient
        .query(repositoriesQuery)
        .responseFetcher(ApolloResponseFetchers.CACHE_AND_NETWORK)
        .watcher()
        .enqueueAndWatch(callback)
  }

  override fun fetchRepositoryDetail(repositoryName: String) {
    val repositoryDetailQuery = GithubRepositoryDetailQuery(
        name = repositoryName,
        pullRequestStates = listOf(PullRequestState.OPEN)
    )

    val callback = createCallback<GithubRepositoryDetailQuery.Data> {
      repositoryDetailSubject.onNext(it)
    }

    apolloClient
        .query(repositoryDetailQuery)
        .responseFetcher(ApolloResponseFetchers.CACHE_AND_NETWORK)
        .watcher()
        .enqueueAndWatch(callback)
  }

  override fun fetchCommits(repositoryName: String) {
    val commitsQuery = GithubRepositoryCommitsQuery(
        name = repositoryName
    )

    val callback = createCallback<GithubRepositoryCommitsQuery.Data> { response ->
      val headCommit = response.data?.viewer?.repository?.ref?.target?.asCommitTarget()
      val commits = headCommit?.history?.edges?.filterNotNull().orEmpty()
      commitsSubject.onNext(commits)
    }

    apolloClient
        .query(commitsQuery)
        .responseFetcher(ApolloResponseFetchers.CACHE_AND_NETWORK)
        .watcher()
        .enqueueAndWatch(callback)
  }

  private fun <T : Operation.Data> createCallback(onResponse: (response: ApolloResponse<T>) -> Unit) =
      object : ApolloCall.Callback<T>() {
        override fun onResponse(response: ApolloResponse<T>) = onResponse(response)

        override fun onFailure(e: ApolloException) {
          exceptionSubject.onNext(e)
        }

        override fun onStatusEvent(event: ApolloCall.StatusEvent) {
          Log.d("ApolloWatcherService", "Apollo Status Event: $event")
        }
      }

  override fun cancelFetching() {
    //TODO: Determine how to cancel this when there's callbacks
  }
}
