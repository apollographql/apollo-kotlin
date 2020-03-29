package com.apollographql.apollo.kmpsample.repositoryDetail

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.apollographql.apollo.kmpsample.KotlinSampleApp
import com.apollographql.apollo.kmpsample.R
import com.apollographql.apollo.kmpsample.commits.CommitsActivity
import com.apollographql.apollo.kmpsample.data.ApolloCoroutinesService
import com.apollographql.apollo.kmpsample.fragment.RepositoryDetail
import kotlinx.android.synthetic.main.activity_repository_detail.buttonCommits
import kotlinx.android.synthetic.main.activity_repository_detail.progressBar
import kotlinx.android.synthetic.main.activity_repository_detail.tvError
import kotlinx.android.synthetic.main.activity_repository_detail.tvRepositoryDescription
import kotlinx.android.synthetic.main.activity_repository_detail.tvRepositoryForks
import kotlinx.android.synthetic.main.activity_repository_detail.tvRepositoryIssues
import kotlinx.android.synthetic.main.activity_repository_detail.tvRepositoryName
import kotlinx.android.synthetic.main.activity_repository_detail.tvRepositoryPullRequests
import kotlinx.android.synthetic.main.activity_repository_detail.tvRepositoryReleases
import kotlinx.android.synthetic.main.activity_repository_detail.tvRepositoryStars
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepositoryDetailActivity : AppCompatActivity() {

  private val dataSource by lazy { ApolloCoroutinesService((application as KotlinSampleApp).apolloClient) }
  private lateinit var job: Job

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_repository_detail)
    val repoName = intent.getStringExtra(REPO_NAME_KEY)
    supportActionBar?.title = repoName

    fetchRepository(repoName)
  }

  private fun fetchRepository(repoName: String) {
    buttonCommits.visibility = View.GONE

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
    progressBar.visibility = View.GONE
    tvError.visibility = View.GONE
    buttonCommits.visibility = View.VISIBLE
    updateUI(repositoryDetail)
  }

  private fun handleError(error: Throwable?) {
    tvError.text = error?.localizedMessage
    tvError.visibility = View.VISIBLE
    progressBar.visibility = View.GONE
    error?.printStackTrace()
  }

  @SuppressLint("SetTextI18n")
  private fun updateUI(repositoryDetail: RepositoryDetail?) {
    repositoryDetail?.run {
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
