package com.apollographql.apollo.kotlinsample.commits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollographql.apollo.kotlinsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kotlinsample.KotlinSampleApp
import com.apollographql.apollo.kotlinsample.R
import com.apollographql.apollo.kotlinsample.data.GitHubDataSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_commits.*

class CommitsActivity : AppCompatActivity() {
  private val adapter = CommitsAdapter()
  private val compositeDisposable = CompositeDisposable()
  private val dataSource: GitHubDataSource by lazy {
    (application as KotlinSampleApp).getDataSource()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_commits)

    val repoName = intent.getStringExtra(REPO_NAME_KEY)!!
    supportActionBar?.title = repoName

    recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    recyclerView.adapter = adapter

    tvError.visibility = GONE
    progressBar.visibility = VISIBLE
    setupDataSource()
    dataSource.fetchCommits(repoName)
  }

  private fun setupDataSource() {
    val successDisposable = dataSource.commits
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::handleCommits)

    val errorDisposable = dataSource.error
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::handleError)

    compositeDisposable.add(successDisposable)
    compositeDisposable.add(errorDisposable)
  }

  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.dispose()
    dataSource.cancelFetching()
  }

  private fun handleCommits(commits: List<GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.CommitTarget.History.Edges>) {
    progressBar.visibility = GONE
    adapter.setItems(commits)
  }

  private fun handleError(error: Throwable?) {
    progressBar.visibility = GONE
    tvError.visibility = VISIBLE
    tvError.text = error?.localizedMessage
  }

  companion object {
    private const val REPO_NAME_KEY = "repoName"

    fun start(context: Context, repoName: String) {
      val intent = Intent(context, CommitsActivity::class.java)
      intent.putExtra(REPO_NAME_KEY, repoName)
      context.startActivity(intent)
    }
  }
}
