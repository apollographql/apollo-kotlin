package com.apollographql.apollo.kmpsample.data

import com.apollographql.apollo.api.Response
import com.apollographql.apollo.kmpsample.GithubRepositoriesQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryDetailQuery

actual class ApolloCoroutinesService {
  private val apolloNetworkClient = ApolloNetworkClient(
      url = BASE_URL,
      headers = mapOf(
          "Accept" to "application/json",
          "Content-Type" to "application/json",
          "Authorization" to "bearer $GITHUB_KEY"
      )
  )

  actual suspend fun fetchRepositories(query: GithubRepositoriesQuery): Response<GithubRepositoriesQuery.Data> =
      with(apolloNetworkClient) {
        query.send()
      }

  actual suspend fun fetchRepositoryDetail(query: GithubRepositoryDetailQuery): Response<GithubRepositoryDetailQuery.Data> =
      with(apolloNetworkClient) {
        query.send()
      }

  actual suspend fun fetchCommits(query: GithubRepositoryCommitsQuery): Response<GithubRepositoryCommitsQuery.Data> =
      with(apolloNetworkClient) {
        query.send()
      }

  companion object {
    private const val GITHUB_KEY = "change_me"
  }
}
