package com.apollographql.apollo.kmpsample.repositories

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo.kmpsample.BuildConfig
import com.apollographql.apollo.kmpsample.KotlinSampleApp
import com.apollographql.apollo.kmpsample.data.ApolloCoroutinesRepository
import com.apollographql.apollo.kmpsample.data.ApolloCoroutinesService
import com.apollographql.apollo.kmpsample.databinding.ActivityMainBinding
import com.apollographql.apollo.kmpsample.repositoryDetail.RepositoryDetailActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

  private val dataSource by lazy { ApolloCoroutinesRepository(ApolloCoroutinesService((application as KotlinSampleApp).apolloClient)) }

  private lateinit var binding: ActivityMainBinding
  private lateinit var repositoriesAdapter: RepositoriesAdapter
  private lateinit var job: Job

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)

    if (BuildConfig.GITHUB_OAUTH_TOKEN == "your_token") {
      binding.tvError.visibility = View.VISIBLE
      binding.tvError.text = "Please replace \"your_token\" in apollo-kotlin-samples/github_token with an actual token.\n\nhttps://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/"
      binding.rvRepositories.visibility = View.GONE
      binding.progressBar.visibility = View.GONE
      return
    }

    binding.tvError.visibility = View.GONE

    binding.rvRepositories.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    binding.rvRepositories.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))
    repositoriesAdapter = RepositoriesAdapter { repositoryFragment ->
      RepositoryDetailActivity.start(this@MainActivity, repositoryFragment.name)
    }
    binding.rvRepositories.adapter = repositoriesAdapter

    setContentView(binding.root)
    fetchRepositories()
  }

  private fun fetchRepositories() {
    job = CoroutineScope(Dispatchers.IO).launch {
      try {
        val repos = dataSource.fetchRepositories()

        withContext(Dispatchers.Main) {
          binding.progressBar.visibility = View.GONE
          binding.rvRepositories.visibility = View.VISIBLE
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
    binding.tvError.text = error?.localizedMessage
    binding.tvError.visibility = View.VISIBLE
    binding.progressBar.visibility = View.GONE
    error?.printStackTrace()
  }

  override fun onDestroy() {
    job.cancel()
    super.onDestroy()
  }
}
