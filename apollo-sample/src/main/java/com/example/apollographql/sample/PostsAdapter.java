package com.example.apollographql.sample;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.AllPosts;

import java.text.NumberFormat;
import java.util.List;

import javax.annotation.Nonnull;

import fragment.PostDetails;

class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {
  @Nonnull private final List<? extends AllPosts.Data.Post> posts;
  @Nonnull private final NumberFormat numberFormat = NumberFormat.getIntegerInstance();

  PostsAdapter(@Nonnull List<? extends AllPosts.Data.Post> posts) {
    this.posts = posts;
  }

  @Override public ViewHolder onCreateViewHolder(@Nonnull ViewGroup parent, int viewType) {
    final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_post, parent, false);
    return new ViewHolder(itemView);
  }

  @Override public void onBindViewHolder(@Nonnull ViewHolder holder, int position) {
    final AllPosts.Data.Post post = posts.get(position);
    final PostDetails details = post.fragments().postDetails();

    holder.title.setText(details.title());

    final PostDetails.Author author = details.author();
    final String byline = (author != null) ? getByline(holder.byline.getResources(), author) : null;
    holder.byline.setText(byline);


    holder.voteCount.setText(numberFormat.format(details.votes()));
  }

  @Override public int getItemCount() {
    return posts.size();
  }

  @Nonnull private static String getByline(@Nonnull Resources resources, @Nonnull PostDetails.Author author) {
    return resources.getString(R.string.byline_format, author.firstName(), author.lastName());
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    final Button upvote;
    final TextView title;
    final TextView byline;
    final TextView voteCount;

    ViewHolder(@Nonnull View itemView) {
      super(itemView);

      upvote = (Button) itemView.findViewById(R.id.upvote);
      title = (TextView) itemView.findViewById(R.id.title);
      byline = (TextView) itemView.findViewById(R.id.byline);
      voteCount = (TextView) itemView.findViewById(R.id.vote_count);
    }
  }
}
