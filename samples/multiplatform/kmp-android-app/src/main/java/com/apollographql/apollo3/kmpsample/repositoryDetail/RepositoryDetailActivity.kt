package com.apollographql.apollo.kmpsample.repositoryDetail

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.apollographql.apollo.kmpsample.commits.CommitsActivity
import com.apollographql.apollo.kmpsample.data.ApolloCoroutinesRepository
import com.apollographql.apollo.kmpsample.databinding.ActivityRepositoryDetailBinding
import com.apollographql.apollo.kmpsample.fragment.RepositoryDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryDetailActivity : AppCompatActivity() {

  private val dataSource by lazy { ApolloCoroutinesRepository() }

  private lateinit var binding: ActivityRepositoryDetailBinding
  private lateinit var job: Job

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityRepositoryDetailBinding.inflate(layoutInflater)
    val repoName = intent.getStringExtra(REPO_NAME_KEY)!!
    supportActionBar?.title = repoName
    setContentView(binding.root)

    fetchRepository(repoName)
  }

  private fun fetchRepository(repoName: String) {
    binding.buttonCommits.visibility = View.GONE

    job = CoroutineScope(Dispatchers.IO).launch {
      try {
        val response = dataSource.fetchRepositoryDetail(repositoryName = repoName)

        withContext(Dispatchers.Main) {
          handleDetailResponse(response)
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          handleError(e)
        }
      }
    }
  }

  private fun handleDetailResponse(repositoryDetail: RepositoryDetail?) {
    binding.progressBar.visibility = View.GONE
    binding.tvError.visibility = View.GONE
    binding.buttonCommits.visibility = View.VISIBLE
    updateUI(repositoryDetail)
  }

  private fun handleError(error: Throwable?) {
    binding.tvError.text = error?.localizedMessage
    binding.tvError.visibility = View.VISIBLE
    binding.progressBar.visibility = View.GONE
    error?.printStackTrace()
  }

  @SuppressLint("SetTextI18n")
  private fun updateUI(repositoryDetail: RepositoryDetail?) {
    repositoryDetail?.run {
      binding.tvRepositoryName.text = name
      binding.tvRepositoryDescription.text = repoDescription
      binding.tvRepositoryForks.text = "$forkCount Forks"
      binding.tvRepositoryIssues.text = "${issues.totalCount} Issues"
      binding.tvRepositoryPullRequests.text = "${pullRequests.totalCount} Pull requests"
      binding.tvRepositoryReleases.text = "${releases.totalCount} Releases"
      binding.tvRepositoryStars.text = "${stargazers.totalCount} Stars"
      binding.buttonCommits.setOnClickListener {
        CommitsActivity.start(this@RepositoryDetailActivity, name)
      }
    }
  }

  override fun onDestroy() {
    job.cancel()
    super.onDestroy()
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
