package com.example.apollographql.sample;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.AllPosts;
import com.example.Upvote;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import fragment.PostDetails;
import io.reactivex.Observer;

class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {
  @Nonnull private final Observer<Integer> upvoteObserver;
  @Nonnull private final List<AllPosts.Data.Post> posts;
  @Nonnull private final NumberFormat numberFormat = NumberFormat.getIntegerInstance();
  @Nonnull private final View.OnClickListener upvoteClickedListener = new View.OnClickListener() {
    @Override public void onClick(View v) {
      upvoteObserver.onNext((Integer)v.getTag());
    }
  };

  PostsAdapter(@Nonnull Observer<Integer> upvoteObserver) {
    this.upvoteObserver = upvoteObserver;
    this.posts = new ArrayList<>();
  }

  @Override public ViewHolder onCreateViewHolder(@Nonnull ViewGroup parent, int viewType) {
    final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_post, parent, false);
    final ViewHolder viewHolder = new ViewHolder(itemView);
    viewHolder.upvote.setOnClickListener(upvoteClickedListener);
    return viewHolder;
  }

  @Override public void onBindViewHolder(@Nonnull ViewHolder holder, int position) {
    final AllPosts.Data.Post post = posts.get(position);
    final PostDetails details = post.fragments().postDetails();

    holder.upvote.setTag(details.id());

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

  void allPosts(@Nonnull List<? extends AllPosts.Data.Post> posts) {
    this.posts.addAll(posts);
  }

  void upvotePost(@Nonnull Upvote.Data.UpvotePost upvotePost) {
    for (int index = 0, max = posts.size(); index < max; ++index) {
      final AllPosts.Data.Post post = posts.get(index);
      final PostDetails details = post.fragments().postDetails();
      final PostDetails upvoteDetails = upvotePost.fragments().postDetails();
      if (details.id() == upvoteDetails.id()) {
        final PostDetails newDetails = PostDetails.CREATOR.create(
            upvoteDetails.id(),
            upvoteDetails.title(),
            upvoteDetails.votes(),
            upvoteDetails.author());
        final AllPosts.Data.Post newData = new AllPosts.Data.Post(new AllPosts.Data.Post.Fragments(newDetails));
        posts.remove(index);
        posts.add(index, newData);

        notifyItemChanged(index);
        break;
      }
    }
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
