package com.apollographql.apollo3.kotlinsample.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.kotlinsample.GithubRepositoriesQuery
import com.apollographql.apollo3.kotlinsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo3.kotlinsample.GithubRepositoryDetailQuery
import com.apollographql.apollo3.kotlinsample.fragment.RepositoryFragment
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * This is a base class defining the required behavior for a data source of GitHub information. We don't care if that data
 * is fetched via RxJava, coroutines, etc. Any implementations of this can fetch data however they want, and post that result
 * to the public Observables that activities can subscribe to for information.
 */
abstract class  GitHubDataSource(protected val apolloClient: ApolloClient) {
  protected val repositoriesSubject: PublishSubject<List<RepositoryFragment>> = PublishSubject.create()
  protected val repositoryDetailSubject: PublishSubject<ApolloResponse<GithubRepositoryDetailQuery.Data>> = PublishSubject.create()
  protected val commitsSubject: PublishSubject<List<GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.CommitTarget.History.Edges>> = PublishSubject.create()
  protected val exceptionSubject: PublishSubject<Throwable> = PublishSubject.create()

  val repositories: Observable<List<RepositoryFragment>> = repositoriesSubject.hide()
  val repositoryDetail: Observable<ApolloResponse<GithubRepositoryDetailQuery.Data>> = repositoryDetailSubject.hide()
  val commits: Observable<List<GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.CommitTarget.History.Edges>> = commitsSubject.hide()
  val error: Observable<Throwable> = exceptionSubject.hide()

  abstract fun fetchRepositories()
  abstract fun fetchRepositoryDetail(repositoryName: String)
  abstract fun fetchCommits(repositoryName: String)
  abstract fun cancelFetching()

  protected fun mapRepositoriesResponseToRepositories(response: ApolloResponse<GithubRepositoriesQuery.Data>): List<RepositoryFragment> {
    return response.data?.viewer?.repositories?.nodes?.mapNotNull { it as RepositoryFragment? } ?: emptyList()
  }
}
