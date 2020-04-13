package com.apollographql.apollo.kmpsample.data

import com.apollographql.apollo.api.Response
import com.apollographql.apollo.kmpsample.GithubRepositoriesQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryDetailQuery

actual class ApolloCoroutinesService {
  actual suspend fun fetchRepositories(query: GithubRepositoriesQuery): Response<GithubRepositoriesQuery.Data> {
    TODO("Not yet implemented")
  }

  actual suspend fun fetchRepositoryDetail(query: GithubRepositoryDetailQuery): Response<GithubRepositoryDetailQuery.Data> {
    TODO("Not yet implemented")
  }

  actual suspend fun fetchCommits(query: GithubRepositoryCommitsQuery): Response<GithubRepositoryCommitsQuery.Data> {
    TODO("Not yet implemented")
  }
}
