package com.apollographql.apollo.kmpsample.data

import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.fragment.RepositoryDetail
import com.apollographql.apollo.kmpsample.fragment.RepositoryFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ApolloiOSRepository(private val repository: ApolloCoroutinesRepository) {

  fun fetchRepositories(success: (List<RepositoryFragment>) -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
      success(repository.fetchRepositories())
    }
  }

  fun fetchRepositoryDetail(repositoryName: String, success: (RepositoryDetail?) -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
      success(repository.fetchRepositoryDetail(repositoryName))
    }
  }

  fun fetchCommits(repositoryName: String, success: (List<GithubRepositoryCommitsQuery.Edge?>) -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
      success(repository.fetchCommits(repositoryName))
    }
  }
}
fun create() = ApolloiOSRepository(ApolloCoroutinesRepository((ApolloCoroutinesService())))
