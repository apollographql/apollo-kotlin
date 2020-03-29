package com.apollographql.apollo.kmpsample.commits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View.GONE
import android.view.View.VISIBLE
import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.KotlinSampleApp
import com.apollographql.apollo.kmpsample.R
import com.apollographql.apollo.kmpsample.data.ApolloCoroutinesService
import kotlinx.android.synthetic.main.activity_commits.recyclerView
import kotlinx.android.synthetic.main.activity_commits.tvError
import kotlinx.android.synthetic.main.activity_main.progressBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommitsActivity : AppCompatActivity() {

  private val adapter = CommitsAdapter()
  private val dataSource by lazy { ApolloCoroutinesService((application as KotlinSampleApp).apolloClient) }
  private lateinit var job: Job

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_commits)

    val repoName = intent.getStringExtra(REPO_NAME_KEY)
    supportActionBar?.title = repoName

    recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    recyclerView.adapter = adapter

    tvError.visibility = GONE
    progressBar.visibility = VISIBLE

    fetchCommits(repoName)
  }

  private fun fetchCommits(repoName: String) {
    job = CoroutineScope(Dispatchers.IO).launch {
      try {
        val commits = dataSource.fetchCommits(repoName)

        withContext(Dispatchers.Main) {
          handleCommits(commits)
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          handleError(e)
        }
      }
    }
  }

  override fun onDestroy() {
    job.cancel()
    super.onDestroy()
  }

  private fun handleCommits(commits: List<GithubRepositoryCommitsQuery.Edge?>) {
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
