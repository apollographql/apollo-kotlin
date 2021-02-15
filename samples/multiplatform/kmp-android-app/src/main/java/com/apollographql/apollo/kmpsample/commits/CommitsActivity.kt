package com.apollographql.apollo.kmpsample.commits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.data.ApolloCoroutinesRepository
import com.apollographql.apollo.kmpsample.databinding.ActivityCommitsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommitsActivity : AppCompatActivity() {

  private val adapter = CommitsAdapter()
  private val dataSource by lazy { ApolloCoroutinesRepository() }
  private lateinit var job: Job

  private lateinit var binding: ActivityCommitsBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCommitsBinding.inflate(layoutInflater)

    val repoName = intent.getStringExtra(REPO_NAME_KEY)!!
    supportActionBar?.title = repoName

    binding.recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    binding.recyclerView.adapter = adapter

    binding.tvError.visibility = GONE
    binding.progressBar.visibility = VISIBLE

    setContentView(binding.root)
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

  private fun handleCommits(commits: List<GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.CommitTarget.History.Edges?>) {
    binding.progressBar.visibility = GONE
    adapter.setItems(commits)
  }

  private fun handleError(error: Throwable?) {
    binding.progressBar.visibility = GONE
    binding.tvError.visibility = VISIBLE
    binding.tvError.text = error?.localizedMessage
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
