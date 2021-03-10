package com.apollographql.apollo3.kotlinsample.repositoryDetail

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.kotlinsample.GithubRepositoryDetailQuery
import com.apollographql.apollo3.kotlinsample.fragment.RepositoryDetail
import com.apollographql.apollo3.kotlinsample.KotlinSampleApp
import com.apollographql.apollo3.kotlinsample.R
import com.apollographql.apollo3.kotlinsample.commits.CommitsActivity
import com.apollographql.apollo3.kotlinsample.data.GitHubDataSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_repository_detail.*

class RepositoryDetailActivity : AppCompatActivity() {

  private val compositeDisposable = CompositeDisposable()
  private val dataSource: GitHubDataSource by lazy {
    (application as KotlinSampleApp).getDataSource()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_repository_detail)

    setupDataSource()

    val repoName = intent.getStringExtra(REPO_NAME_KEY)!!
    supportActionBar?.title = repoName

    fetchRepository(repoName)
  }

  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.clear()
    dataSource.cancelFetching()
  }

  private fun setupDataSource() {
    val successDisposable = dataSource.repositoryDetail
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::handleDetailResponse)

    val errorDisposable = dataSource.error
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::handleError)

    compositeDisposable.add(successDisposable)
    compositeDisposable.add(errorDisposable)
  }

  private fun handleDetailResponse(response: ApolloResponse<GithubRepositoryDetailQuery.Data>) {
    progressBar.visibility = View.GONE
    tvError.visibility = View.GONE
    buttonCommits.visibility = View.VISIBLE
    updateUI(response)
  }

  private fun handleError(error: Throwable?) {
    tvError.text = error?.localizedMessage
    tvError.visibility = View.VISIBLE
    progressBar.visibility = View.GONE
    error?.printStackTrace()
  }

  private fun fetchRepository(repoName: String) {
    buttonCommits.visibility = View.GONE

    dataSource.fetchRepositoryDetail(repositoryName = repoName)
  }

  @SuppressLint("SetTextI18n")
  private fun updateUI(response: ApolloResponse<GithubRepositoryDetailQuery.Data>) {
    (response.data?.viewer?.repository as RepositoryDetail?)?.run {
      tvRepositoryName.text = name
      tvRepositoryDescription.text = description
      tvRepositoryForks.text = "$forkCount Forks"
      tvRepositoryIssues.text = "${issues.totalCount} Issues"
      tvRepositoryPullRequests.text = "${pullRequests.totalCount} Pull requests"
      tvRepositoryReleases.text = "${releases.totalCount} Releases"
      tvRepositoryStars.text = "${stargazers.totalCount} Stars"
      buttonCommits.setOnClickListener {
        CommitsActivity.start(this@RepositoryDetailActivity, name)
      }
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
