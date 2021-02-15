package com.apollographql.apollo.kotlinsample.commits

import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo.kotlinsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kotlinsample.R
import kotlinx.android.synthetic.main.item_commit.view.*

class CommitsAdapter : RecyclerView.Adapter<CommitsAdapter.ViewHolder>() {

  private var data: List<GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.CommitTarget.History.Edges>? = null

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_commit, parent, false)
    )
  }

  override fun getItemCount() = data?.size ?:0

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(data!![position])
  }


  fun setItems(data: List<GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.CommitTarget.History.Edges>) {
    this.data = data
    notifyDataSetChanged()
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(commit: GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.CommitTarget.History.Edges) {
      itemView.run {
        tvCommitSha1.text = commit.node?.abbreviatedOid
        val headline = "${commit.node?.author?.email}: ${commit.node?.messageHeadline}"
        tvCommitMessage.visibility = VISIBLE
        tvCommitMessage.text = headline
      }
    }
  }
}
