package com.apollographql.apollo.kmpsample.data

import com.apollographql.apollo.api.Response
import com.apollographql.apollo.kmpsample.GithubRepositoriesQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryDetailQuery

expect class ApolloCoroutinesService {
  suspend fun fetchRepositories(query: GithubRepositoriesQuery): Response<GithubRepositoriesQuery.Data>
  suspend fun fetchRepositoryDetail(query: GithubRepositoryDetailQuery): Response<GithubRepositoryDetailQuery.Data>
  suspend fun fetchCommits(query: GithubRepositoryCommitsQuery): Response<GithubRepositoryCommitsQuery.Data>
}
