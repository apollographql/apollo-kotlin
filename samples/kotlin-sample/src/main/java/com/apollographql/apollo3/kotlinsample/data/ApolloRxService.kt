package com.apollographql.apollo.kotlinsample.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.kotlinsample.GithubRepositoriesQuery
import com.apollographql.apollo.kotlinsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kotlinsample.GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.Companion.asCommitTarget
import com.apollographql.apollo.kotlinsample.GithubRepositoryDetailQuery
import com.apollographql.apollo.kotlinsample.type.OrderDirection
import com.apollographql.apollo.kotlinsample.type.PullRequestState
import com.apollographql.apollo.kotlinsample.type.RepositoryOrderField
import com.apollographql.apollo.rx2.rxQuery
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * An implementation of a [GitHubDataSource] that shows how we can use RxJava to make our apollo requests.
 */
class ApolloRxService(
    apolloClient: ApolloClient,
    private val compositeDisposable: CompositeDisposable = CompositeDisposable(),
    private val processScheduler: Scheduler = Schedulers.io(),
    private val resultScheduler: Scheduler = AndroidSchedulers.mainThread()
) : GitHubDataSource(apolloClient) {
  override fun fetchRepositories() {
    val repositoriesQuery = GithubRepositoriesQuery(
        repositoriesCount = 50,
        orderBy = RepositoryOrderField.UPDATED_AT,
        orderDirection = OrderDirection.DESC
    )

    val disposable = apolloClient.rxQuery(repositoriesQuery)
        .subscribeOn(processScheduler)
        .observeOn(resultScheduler)
        .map(this::mapRepositoriesResponseToRepositories)
        .subscribe(
            repositoriesSubject::onNext,
            exceptionSubject::onNext
        )

    compositeDisposable.add(disposable)
  }

  override fun fetchRepositoryDetail(repositoryName: String) {
    val repositoryDetailQuery = GithubRepositoryDetailQuery(
        name = repositoryName,
        pullRequestStates = listOf(PullRequestState.OPEN)
    )

    val disposable = apolloClient.rxQuery(repositoryDetailQuery)
        .subscribeOn(processScheduler)
        .observeOn(resultScheduler)
        .subscribe(
            repositoryDetailSubject::onNext,
            exceptionSubject::onNext
        )

    compositeDisposable.add(disposable)
  }

  override fun fetchCommits(repositoryName: String) {
    val commitsQuery = GithubRepositoryCommitsQuery(
        name = repositoryName
    )

    val disposable = apolloClient
        .rxQuery(commitsQuery) {
          httpCachePolicy(HttpCachePolicy.NETWORK_FIRST)
        }
        .subscribeOn(processScheduler)
        .observeOn(resultScheduler)
        .map { response ->
          val headCommit = response.data?.viewer?.repository?.ref?.target?.asCommitTarget()
          headCommit?.history?.edges?.filterNotNull().orEmpty()
        }
        .subscribe(
            commitsSubject::onNext,
            exceptionSubject::onNext
        )

    compositeDisposable.add(disposable)
  }

  override fun cancelFetching() {
    compositeDisposable.dispose()
  }
}
