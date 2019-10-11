package com.apollographql.apollo.kotlinsample.commits

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.apollographql.apollo.kotlinsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kotlinsample.R
import kotlinx.android.synthetic.main.item_commit.view.*

class CommitsAdapter : RecyclerView.Adapter<CommitsAdapter.ViewHolder>() {

  private var data: List<GithubRepositoryCommitsQuery.Edge>? = null

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_commit, parent, false)
    )
  }

  override fun getItemCount() = data?.size ?:0

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(data!![position])
  }


  fun setItems(data: List<GithubRepositoryCommitsQuery.Edge>) {
    this.data = data
    notifyDataSetChanged()
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(commit: GithubRepositoryCommitsQuery.Edge) {
      itemView.run {
        tvCommitSha1.text = commit.node()?.abbreviatedOid()
        val headline = "${commit.node()?.author()?.email()}: ${commit.node()?.messageHeadline()}"
        if (headline == null) {
          tvCommitMessage.visibility = View.GONE
        } else {
          tvCommitMessage.visibility = VISIBLE
          tvCommitMessage.text = headline
        }
      }
    }
  }
}