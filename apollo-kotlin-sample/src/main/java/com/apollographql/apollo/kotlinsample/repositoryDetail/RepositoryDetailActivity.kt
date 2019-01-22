package com.apollographql.apollo.kotlinsample.repositoryDetail

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.kotlinsample.GithubRepositoryDetailQuery
import com.apollographql.apollo.kotlinsample.KotlinSampleApp
import com.apollographql.apollo.kotlinsample.R
import com.apollographql.apollo.kotlinsample.type.PullRequestState
import com.apollographql.apollo.rx2.Rx2Apollo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_repository_detail.*

class RepositoryDetailActivity : AppCompatActivity() {

  private lateinit var apolloClient: ApolloClient
  private val compositeDisposable = CompositeDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_repository_detail)

    apolloClient = (application as KotlinSampleApp).apolloClient

    val repoName = intent.getStringExtra(REPO_NAME_KEY)
    supportActionBar?.title = repoName

    fetchRepository(repoName)
  }

  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.clear()
  }

  private fun fetchRepository(repoName: String) {
    val repositoryDetailQuery = GithubRepositoryDetailQuery.builder()
      .name(repoName)
      .pullRequestStates(listOf(PullRequestState.OPEN))
      .build()

    val call = apolloClient
      .query(repositoryDetailQuery)
      .httpCachePolicy(HttpCachePolicy.CACHE_FIRST)

    compositeDisposable.add(
      Rx2Apollo.from(call)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
          { response: Response<GithubRepositoryDetailQuery.Data> ->
            progressBar.visibility = View.GONE
            tvError.visibility = View.GONE
            updateUI(response)
          },
          { t: Throwable ->
            tvError.text = t.localizedMessage
            tvError.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            t.printStackTrace()
          }
        )
    )
  }

  @SuppressLint("SetTextI18n")
  private fun updateUI(response: Response<GithubRepositoryDetailQuery.Data>) {
    response.data()?.viewer()?.repository()?.fragments()?.repositoryDetail()?.run {
      tvRepositoryName.text = name()
      tvRepositoryDescription.text = description()
      tvRepositoryForks.text = "${forkCount()} Forks"
      tvRepositoryIssues.text = "${issues().totalCount()} Issues"
      tvRepositoryPullRequests.text = "${pullRequests().totalCount()} Pull requests"
      tvRepositoryReleases.text = "${releases().totalCount()} Releases"
      tvRepositoryStars.text = "${stargazers().totalCount()} Stars"
    }
  }

  companion object {
    private const val REPO_NAME_KEY = "repoName"

    fun start(context: Context, repoName: String) {
      val intent = Intent(context, RepositoryDetailActivity::class.java)
      intent.putExtra(REPO_NAME_KEY, repoName)
      context.startActivity(intent)
    }
  }
}
