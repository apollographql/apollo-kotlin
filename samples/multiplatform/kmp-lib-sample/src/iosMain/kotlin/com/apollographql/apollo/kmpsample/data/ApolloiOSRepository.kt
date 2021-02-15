package com.apollographql.apollo.kmpsample.data

import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.fragment.RepositoryDetail
import com.apollographql.apollo.kmpsample.fragment.RepositoryFragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch

@InternalCoroutinesApi
class ApolloiOSRepository(private val repository: ApolloCoroutinesRepository) {

  fun fetchRepositories(success: (List<RepositoryFragment>) -> Unit) {
    GlobalScope.launch(MainLoopDispatcher) {
      success(repository.fetchRepositories())
    }
  }

  fun fetchRepositoryDetail(repositoryName: String, success: (RepositoryDetail?) -> Unit) {
    GlobalScope.launch(MainLoopDispatcher) {
      success(repository.fetchRepositoryDetail(repositoryName))
    }
  }

  fun fetchCommits(repositoryName: String, success: (List<GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.CommitTarget.History.Edges?>) -> Unit) {
    GlobalScope.launch(MainLoopDispatcher) {
      success(repository.fetchCommits(repositoryName))
    }
  }
}

@InternalCoroutinesApi
fun create() = ApolloiOSRepository(ApolloCoroutinesRepository())
