package com.apollographql.apollo.kmpsample.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.toDeferred
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.kmpsample.GithubRepositoriesQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryDetailQuery
import kotlinx.coroutines.invoke

actual class ApolloCoroutinesService(private val apolloClient: ApolloClient) {

  actual suspend fun fetchRepositories(query: GithubRepositoriesQuery): Response<GithubRepositoriesQuery.Data> {
    return apolloClient.query(query).toDeferred().await()
  }

  actual suspend fun fetchRepositoryDetail(query: GithubRepositoryDetailQuery): Response<GithubRepositoryDetailQuery.Data> {
    return apolloClient.query(query).toDeferred().await()
  }

  actual suspend fun fetchCommits(query: GithubRepositoryCommitsQuery): Response<GithubRepositoryCommitsQuery.Data> {
    return apolloClient.query(query).toDeferred().await()
  }
}
