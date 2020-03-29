package com.apollographql.apollo.kmpsample.repositories

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.LinearLayout
import com.apollographql.apollo.kmpsample.BuildConfig
import com.apollographql.apollo.kmpsample.KotlinSampleApp
import com.apollographql.apollo.kmpsample.R
import com.apollographql.apollo.kmpsample.data.ApolloCoroutinesService
import com.apollographql.apollo.kmpsample.repositoryDetail.RepositoryDetailActivity
import kotlinx.android.synthetic.main.activity_main.progressBar
import kotlinx.android.synthetic.main.activity_main.rvRepositories
import kotlinx.android.synthetic.main.activity_main.tvError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

  private val dataSource by lazy { ApolloCoroutinesService((application as KotlinSampleApp).apolloClient) }
  private lateinit var repositoriesAdapter: RepositoriesAdapter
  private lateinit var job: Job

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

    rvRepositories.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
    rvRepositories.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))
    repositoriesAdapter = RepositoriesAdapter { repositoryFragment ->
      RepositoryDetailActivity.start(this@MainActivity, repositoryFragment.name)
    }
    rvRepositories.adapter = repositoriesAdapter

    fetchRepositories()
  }

  private fun fetchRepositories() {
    job = CoroutineScope(Dispatchers.IO).launch {
      try {
        val repos = dataSource.fetchRepositories()

        withContext(Dispatchers.Main) {
          progressBar.visibility = View.GONE
          rvRepositories.visibility = View.VISIBLE
          repositoriesAdapter.setItems(repos)
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          handleError(e)
        }
      }
    }
  }

  private fun handleError(error: Throwable?) {
    tvError.text = error?.localizedMessage
    tvError.visibility = View.VISIBLE
    progressBar.visibility = View.GONE
    error?.printStackTrace()
  }

  override fun onDestroy() {
    job.cancel()
    super.onDestroy()
  }
}
