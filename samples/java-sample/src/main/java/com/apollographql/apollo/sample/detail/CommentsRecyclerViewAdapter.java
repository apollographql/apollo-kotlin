package com.apollographql.apollo.sample.detail;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.apollographql.apollo.sample.R;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

class CommentsRecyclerViewAdapter extends RecyclerView.Adapter<CommentsRecyclerViewAdapter.ViewHolder> {
  private List<String> items = new ArrayList<>();

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    final View itemView = layoutInflater.inflate(R.layout.list_view_item_repo_comment, parent, false);
    return new CommentsRecyclerViewAdapter.ViewHolder(itemView);
  }

  @Override public void onBindViewHolder(ViewHolder holder, int position) {
    holder.bindView(items.get(position));
  }

  @Override public int getItemCount() {
    return items.size();
  }

  void setItems(@NotNull List<String> items) {
    this.items = new ArrayList<>(items);
    notifyDataSetChanged();
  }

  void addItem(@NotNull String comment) {
    items.add(0, comment);
    notifyItemInserted(0);
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    ViewHolder(View itemView) {
      super(itemView);
    }

    void bindView(String content) {
      ((TextView) itemView).setText(content);
    }
  }
}
