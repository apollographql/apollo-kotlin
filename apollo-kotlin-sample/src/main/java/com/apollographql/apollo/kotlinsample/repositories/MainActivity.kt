package com.apollographql.apollo.kotlinsample.repositories

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.LinearLayout
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloCallback
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.kotlinsample.GithubRepositoriesQuery
import com.apollographql.apollo.kotlinsample.KotlinSampleApp
import com.apollographql.apollo.kotlinsample.R
import com.apollographql.apollo.kotlinsample.fragment.RepositoryFragment
import com.apollographql.apollo.kotlinsample.repositoryDetail.RepositoryDetailActivity
import com.apollographql.apollo.kotlinsample.type.OrderDirection
import com.apollographql.apollo.kotlinsample.type.RepositoryOrderField
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

  private lateinit var apolloClient: ApolloClient
  private lateinit var repositoriesAdapter: RepositoriesAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    apolloClient = (application as KotlinSampleApp).apolloClient

    rvRepositories.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
    rvRepositories.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))
    repositoriesAdapter = RepositoriesAdapter { repositoryFragment ->
      RepositoryDetailActivity.start(this@MainActivity, repositoryFragment.name())
    }
    rvRepositories.adapter = repositoriesAdapter

    fetchRepositories()
  }

  private fun fetchRepositories() {
    val repositoriesQuery = GithubRepositoriesQuery.builder()
      .repositoriesCount(50)
      .orderBy(RepositoryOrderField.UPDATED_AT)
      .orderDirection(OrderDirection.DESC)
      .build()

    val callback = ApolloCallback.wrap(object : ApolloCall.Callback<GithubRepositoriesQuery.Data>() {
      override fun onResponse(response: Response<GithubRepositoriesQuery.Data>) {
        progressBar.visibility = View.GONE
        rvRepositories.visibility = View.VISIBLE
        val repositories = mapResponseToRepositories(response)
        repositoriesAdapter.setItems(repositories)
      }

      override fun onFailure(e: ApolloException) {
        tvError.text = e.localizedMessage
        tvError.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        e.printStackTrace()
      }

    }, Handler(Looper.getMainLooper()))

    apolloClient
      .query(repositoriesQuery)
      .httpCachePolicy(HttpCachePolicy.CACHE_FIRST)
      .enqueue(callback)
  }

  private fun mapResponseToRepositories(response: Response<GithubRepositoriesQuery.Data>): List<RepositoryFragment> {
    return response.data()?.viewer()?.repositories()?.nodes()?.map { it.fragments().repositoryFragment() } ?: emptyList()
  }
}
