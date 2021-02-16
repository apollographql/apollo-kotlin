package com.apollographql.apollo.kmpsample.commits

import android.view.LayoutInflater
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.databinding.ItemCommitBinding

class CommitsAdapter : RecyclerView.Adapter<CommitsAdapter.ViewHolder>() {

  private var data: List<GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.CommitTarget.History.Edges?>? = null

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return ViewHolder(ItemCommitBinding.inflate(inflater, parent, false))
  }

  override fun getItemCount() = data?.size ?: 0

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(data!![position])
  }

  fun setItems(data: List<GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.CommitTarget.History.Edges?>) {
    this.data = data
    notifyDataSetChanged()
  }

  class ViewHolder(private val binding: ItemCommitBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(commit: GithubRepositoryCommitsQuery.Data.Viewer.Repository.Ref.Target.CommitTarget.History.Edges?) {
      binding.run {
        tvCommitSha1.text = commit?.node?.abbreviatedOid
        val headline = "${commit?.node?.author?.email}: ${commit?.node?.messageHeadline}"
        tvCommitMessage.visibility = VISIBLE
        tvCommitMessage.text = headline
      }
    }
  }
}
