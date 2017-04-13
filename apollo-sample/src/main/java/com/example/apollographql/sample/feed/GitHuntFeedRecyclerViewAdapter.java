package com.example.apollographql.sample.feed;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.FeedQuery;
import com.example.apollographql.sample.R;
import com.example.fragment.RepositoryFragment;
import com.squareup.picasso.Picasso;

import java.util.Collections;
import java.util.List;

class GitHuntFeedRecyclerViewAdapter extends
    RecyclerView.Adapter<GitHuntFeedRecyclerViewAdapter.FeedItemViewHolder> {

  private List<FeedQuery.Data.FeedEntry> feed = Collections.emptyList();
  private GitHuntNavigator navigator;
  int visitCount = 0;

  public void setFeed(List<FeedQuery.Data.FeedEntry> feed, GitHuntNavigator navigator) {
    this.feed = feed;
    this.notifyDataSetChanged();
    this.navigator = navigator;
  }

  @Override
  public FeedItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    final View itemView = layoutInflater.inflate(R.layout.item_githunt_entry, parent, false);

    return new FeedItemViewHolder(itemView);
  }

  @Override public void onBindViewHolder(FeedItemViewHolder holder, int position) {
    final FeedQuery.Data.FeedEntry feedEntry = this.feed.get(position);
    holder.setFeedItem(feedEntry, navigator);
    if (feedEntry.id() == 1) {
      holder.upvoteCount.setText("" + 1);
    }
  }

  @Override public int getItemCount() {
    return feed.size();
  }

  static class FeedItemViewHolder extends RecyclerView.ViewHolder {

    private TextView repositoryTitle;
    private TextView upvoteCount;
    private TextView submittedBy;
    private ImageView repositoryImage;
    private View feedEntryContainer;

    public FeedItemViewHolder(View itemView) {
      super(itemView);
      repositoryImage = (ImageView) itemView.findViewById(R.id.iv_repository_icon);
      repositoryTitle = (TextView) itemView.findViewById(R.id.tv_repository_name);
      submittedBy = (TextView) itemView.findViewById(R.id.tv_submitted_by);
      feedEntryContainer = itemView.findViewById(R.id.feed_entry_container);
      upvoteCount = (TextView) itemView.findViewById(R.id.tv_upvotes);
    }

    public void setFeedItem(FeedQuery.Data.FeedEntry feedItem, final GitHuntNavigator navigator) {
      final RepositoryFragment repositoryFragment = feedItem.repository().fragments().repositoryFragment();
      repositoryTitle.setText(repositoryFragment.full_name());
      Picasso.with(itemView.getContext()).load(repositoryFragment.owner().avatar_url()).into(repositoryImage);
      upvoteCount.setText("Votes: " + feedItem.vote().vote_value());
      submittedBy.setText(feedItem.postedBy().login());
      feedEntryContainer.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          navigator.startGitHuntActivity(repositoryFragment.full_name());
        }
      });

    }
  }
}
