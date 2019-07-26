package com.apollographql.apollo.sample.feed;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.apollographql.apollo.sample.FeedQuery;
import com.apollographql.apollo.sample.R;
import com.apollographql.apollo.sample.fragment.RepositoryFragment;

import java.util.Collections;
import java.util.List;

class GitHuntFeedRecyclerViewAdapter extends
    RecyclerView.Adapter<GitHuntFeedRecyclerViewAdapter.FeedItemViewHolder> {

  private List<FeedQuery.FeedEntry> feed = Collections.emptyList();
  private GitHuntNavigator navigator;

  GitHuntFeedRecyclerViewAdapter(GitHuntNavigator navigator) {
    this.navigator = navigator;
  }

  public void setFeed(List<FeedQuery.FeedEntry> feed) {
    this.feed = feed;
    this.notifyDataSetChanged();
  }

  @Override
  public FeedItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    final View itemView = layoutInflater.inflate(R.layout.item_githunt_entry, parent, false);

    return new FeedItemViewHolder(itemView);
  }

  @Override public void onBindViewHolder(FeedItemViewHolder holder, int position) {
    final FeedQuery.FeedEntry feedEntry = this.feed.get(position);
    holder.setFeedItem(feedEntry, navigator);
  }

  @Override public int getItemCount() {
    return feed.size();
  }

  static class FeedItemViewHolder extends RecyclerView.ViewHolder {

    private TextView repositoryTitle;
    private View feedEntryContainer;

    FeedItemViewHolder(View itemView) {
      super(itemView);
      repositoryTitle = (TextView) itemView.findViewById(R.id.tv_repository_name);
      feedEntryContainer = itemView.findViewById(R.id.feed_entry_container);
    }

    void setFeedItem(FeedQuery.FeedEntry feedItem, final GitHuntNavigator navigator) {
      final RepositoryFragment repositoryFragment = feedItem.repository().fragments().repositoryFragment();
      repositoryTitle.setText(repositoryFragment.full_name());
      feedEntryContainer.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          navigator.startGitHuntActivity(repositoryFragment.full_name());
        }
      });

    }
  }
}
