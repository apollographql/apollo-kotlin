package com.apollographql.apollo3.kmpsample.repositories

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo3.kmpsample.databinding.ItemRepositoryBinding
import com.apollographql.apollo3.kmpsample.fragment.RepositoryFragment

class RepositoriesAdapter(
    private val onClick: (RepositoryFragment) -> Unit
) : RecyclerView.Adapter<RepositoriesAdapter.ViewHolder>() {

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
        if (repositoryFragment.repoDescription == null) {
          tvRepositoryDescription.visibility = View.GONE
        } else {
          tvRepositoryDescription.text = repositoryFragment.repoDescription
        }

        rootLayout.setOnClickListener {
          onClick(repositoryFragment)
        }
      }
    }
  }
}
