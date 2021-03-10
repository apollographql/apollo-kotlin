package com.apollographql.apollo3.kotlinsample.data

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.cache.http.HttpCachePolicy
import com.apollographql.apollo3.exception.ApolloException
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
class ApolloCallbackService(apolloClient: ApolloClient) : GitHubDataSource(apolloClient) {
  override fun fetchRepositories() {
    val repositoriesQuery = GithubRepositoriesQuery(
        repositoriesCount = 50,
        orderBy = RepositoryOrderField.UPDATED_AT,
        orderDirection = OrderDirection.DESC
    )

    val callback = object : ApolloCall.Callback<GithubRepositoriesQuery.Data>() {
      override fun onFailure(e: ApolloException) {
        exceptionSubject.onNext(e)
      }

      override fun onResponse(response: ApolloResponse<GithubRepositoriesQuery.Data>) {
        repositoriesSubject.onNext(mapRepositoriesResponseToRepositories(response))
      }
    }

    apolloClient
        .query(repositoriesQuery)
        .httpCachePolicy(HttpCachePolicy.CACHE_FIRST)
        .enqueue(callback)
  }

  override fun fetchRepositoryDetail(repositoryName: String) {
    val repositoryDetailQuery = GithubRepositoryDetailQuery(
        name = repositoryName,
        pullRequestStates = listOf(PullRequestState.OPEN)
    )

    val callback = object : ApolloCall.Callback<GithubRepositoryDetailQuery.Data>() {
      override fun onFailure(e: ApolloException) {
        exceptionSubject.onNext(e)
      }

      override fun onResponse(response: ApolloResponse<GithubRepositoryDetailQuery.Data>) {
        repositoryDetailSubject.onNext(response)
      }
    }

    apolloClient
        .query(repositoryDetailQuery)
        .httpCachePolicy(HttpCachePolicy.CACHE_FIRST)
        .enqueue(callback)
  }

  override fun fetchCommits(repositoryName: String) {
    val commitsQuery = GithubRepositoryCommitsQuery(
        name = repositoryName
    )

    val callback = object : ApolloCall.Callback<GithubRepositoryCommitsQuery.Data>() {
      override fun onFailure(e: ApolloException) {
        exceptionSubject.onNext(e)
      }

      override fun onResponse(response: ApolloResponse<GithubRepositoryCommitsQuery.Data>) {
        val headCommit = response.data?.viewer?.repository?.ref?.target?.asCommitTarget()
        val commits = headCommit?.history?.edges?.filterNotNull().orEmpty()
        commitsSubject.onNext(commits)
      }
    }

    apolloClient
        .query(commitsQuery)
        .httpCachePolicy(HttpCachePolicy.CACHE_FIRST)
        .enqueue(callback)
  }

  override fun cancelFetching() {
    //TODO: Determine how to cancel this when there's callbacks
  }
}
