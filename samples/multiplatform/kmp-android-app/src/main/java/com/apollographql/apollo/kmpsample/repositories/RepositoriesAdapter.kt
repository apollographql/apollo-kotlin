package com.apollographql.apollo.kmpsample.repositories

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.apollographql.apollo.kmpsample.databinding.ItemRepositoryBinding
import com.apollographql.apollo.kmpsample.fragment.RepositoryFragment

class RepositoriesAdapter(private val onClick: (RepositoryFragment) -> Unit) : RecyclerView.Adapter<RepositoriesAdapter.ViewHolder>() {

  private var data: List<RepositoryFragment> = ArrayList()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return ViewHolder(ItemRepositoryBinding.inflate(inflater, parent, false))
  }

  override fun getItemCount() = data.size

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(data[position], onClick)
  }

  fun setItems(data: List<RepositoryFragment>) {
    this.data = data
    notifyDataSetChanged()
  }

  class ViewHolder(private val binding: ItemRepositoryBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(repositoryFragment: RepositoryFragment, onClick: (RepositoryFragment) -> Unit) {
      binding.run {
        tvRepositoryName.text = repositoryFragment.name
        if (repositoryFragment.description == null) {
          tvRepositoryDescription.visibility = View.GONE
        } else {
          tvRepositoryDescription.text = repositoryFragment.description
        }

        rootLayout.setOnClickListener {
          onClick(repositoryFragment)
        }
      }
    }
  }
}
