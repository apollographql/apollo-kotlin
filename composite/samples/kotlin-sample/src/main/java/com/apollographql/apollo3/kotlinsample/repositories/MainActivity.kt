package com.apollographql.apollo3.kotlinsample.repositories

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo3.kotlinsample.BuildConfig
import com.apollographql.apollo3.kotlinsample.KotlinSampleApp
import com.apollographql.apollo3.kotlinsample.R
import com.apollographql.apollo3.kotlinsample.data.GitHubDataSource
import com.apollographql.apollo3.kotlinsample.fragment.RepositoryFragment
import com.apollographql.apollo3.kotlinsample.repositoryDetail.RepositoryDetailActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.progressBar
import kotlinx.android.synthetic.main.activity_main.rvRepositories
import kotlinx.android.synthetic.main.activity_main.tvError

class MainActivity : AppCompatActivity() {

  private lateinit var repositoriesAdapter: RepositoriesAdapter
  private val compositeDisposable = CompositeDisposable()
  private val dataSource: GitHubDataSource by lazy {
    (application as KotlinSampleApp).getDataSource()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    if (BuildConfig.GITHUB_OAUTH_TOKEN == "your_token") {
      tvError.visibility = View.VISIBLE
      tvError.text = "Please replace \"your_token\" in apollo-kotlin-samples/github_token with an actual token.\n\nhttps://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/"
      rvRepositories.visibility = View.GONE
      progressBar.visibility = View.GONE
      return
    }

    tvError.visibility = View.GONE

    rvRepositories.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    rvRepositories.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
    repositoriesAdapter = RepositoriesAdapter { repositoryFragment ->
      RepositoryDetailActivity.start(this@MainActivity, repositoryFragment.name)
    }
    rvRepositories.adapter = repositoriesAdapter

    setupDataSource()
    fetchRepositories()
  }

  private fun setupDataSource() {
    val successDisposable = dataSource.repositories
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::handleRepositories)

    val errorDisposable = dataSource.error
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::handleError)

    compositeDisposable.add(successDisposable)
    compositeDisposable.add(errorDisposable)
  }

  private fun handleRepositories(repos: List<RepositoryFragment>) {
    progressBar.visibility = View.GONE
    rvRepositories.visibility = View.VISIBLE
    repositoriesAdapter.setItems(repos)
  }

  private fun handleError(error: Throwable?) {
    tvError.text = error?.localizedMessage
    tvError.visibility = View.VISIBLE
    progressBar.visibility = View.GONE
    error?.printStackTrace()
  }

  private fun fetchRepositories() {
    dataSource.fetchRepositories()
  }

  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.dispose()
    dataSource.cancelFetching()
  }
}
