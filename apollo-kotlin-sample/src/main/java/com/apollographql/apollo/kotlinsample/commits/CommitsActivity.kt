package com.apollographql.apollo.kotlinsample.commits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import com.apollographql.apollo.coroutines.toDeferred
import com.apollographql.apollo.kotlinsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kotlinsample.KotlinSampleApp
import com.apollographql.apollo.kotlinsample.R
import com.apollographql.apollo.kotlinsample.repositories.CommitsAdapter
import kotlinx.android.synthetic.main.activity_commits.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CommitsActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_commits)

    val apolloClient = (application as KotlinSampleApp).apolloClient

    val repoName = intent.getStringExtra(CommitsActivity.REPO_NAME_KEY)
    supportActionBar?.title = repoName

    val adapter = CommitsAdapter()

    recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    recyclerView.adapter = adapter

    tvError.visibility = GONE
    progressBar.visibility = VISIBLE
    GlobalScope.launch(Dispatchers.Main) {
      try {
        val response = apolloClient.query(GithubRepositoryCommitsQuery(repoName)).toDeferred().await()
        val headCommit = response.data()?.viewer()?.repository()?.ref()?.target() as? GithubRepositoryCommitsQuery.AsCommit
        adapter.setItems(headCommit?.history()?.edges()!!)
      } catch (e: Exception) {
        tvError.visibility = View.VISIBLE
        tvError.text = e.localizedMessage
      } finally {
        progressBar.visibility = GONE
      }
    }
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