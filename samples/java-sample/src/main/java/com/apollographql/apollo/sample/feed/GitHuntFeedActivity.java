package com.apollographql.apollo.sample.feed;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloCallback;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.sample.FeedQuery;
import com.apollographql.apollo.sample.GitHuntApplication;
import com.apollographql.apollo.sample.R;
import com.apollographql.apollo.sample.detail.GitHuntEntryDetailActivity;
import com.apollographql.apollo.sample.type.FeedType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo.sample.FeedQuery.FeedEntry;

public class GitHuntFeedActivity extends AppCompatActivity implements GitHuntNavigator {

  static final String TAG = GitHuntFeedActivity.class.getSimpleName();

  private static final int FEED_SIZE = 20;

  GitHuntApplication application;
  ViewGroup content;
  ProgressBar progressBar;
  GitHuntFeedRecyclerViewAdapter feedAdapter;
  Handler uiHandler = new Handler(Looper.getMainLooper());
  ApolloCall<FeedQuery.Data> githuntFeedCall;


  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_githunt_feed);
    application = (GitHuntApplication) getApplication();

    content = (ViewGroup) findViewById(R.id.content_holder);
    progressBar = (ProgressBar) findViewById(R.id.loading_bar);

    RecyclerView feedRecyclerView = (RecyclerView) findViewById(R.id.rv_feed_list);
    feedAdapter = new GitHuntFeedRecyclerViewAdapter(this);
    feedRecyclerView.setAdapter(feedAdapter);
    feedRecyclerView.setLayoutManager(new LinearLayoutManager(this));

    fetchFeed();
  }

  private ApolloCall.Callback<FeedQuery.Data> dataCallback
      = new ApolloCallback<>(new ApolloCall.Callback<FeedQuery.Data>() {
    @Override public void onResponse(@NotNull Response<FeedQuery.Data> response) {
      feedAdapter.setFeed(feedResponseToEntriesWithRepositories(response));
      progressBar.setVisibility(View.GONE);
      content.setVisibility(View.VISIBLE);
    }

    @Override public void onFailure(@NotNull ApolloException e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }, uiHandler);

  List<FeedEntry> feedResponseToEntriesWithRepositories(Response<FeedQuery.Data> response) {
    List<FeedEntry> feedEntriesWithRepos = new ArrayList<>();
    final FeedQuery.Data responseData = response.data();
    if (responseData == null) {
      return Collections.emptyList();
    }
    final List<FeedEntry> feedEntries = responseData.feedEntries();
    if (feedEntries == null) {
      return Collections.emptyList();
    }
    for (FeedEntry entry : feedEntries) {
      if (entry.repository() != null) {
        feedEntriesWithRepos.add(entry);
      }
    }
    return feedEntriesWithRepos;
  }

  private void fetchFeed() {
    final FeedQuery feedQuery = FeedQuery.builder()
        .limit(FEED_SIZE)
        .type(FeedType.HOT)
        .build();
    githuntFeedCall = application.apolloClient()
        .query(feedQuery)
        .responseFetcher(ApolloResponseFetchers.NETWORK_FIRST);
    githuntFeedCall.enqueue(dataCallback);
  }

  @Override public void startGitHuntActivity(String repoFullName) {
    final Intent intent = GitHuntEntryDetailActivity.newIntent(this, repoFullName);
    startActivity(intent);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    if (githuntFeedCall != null) {
      githuntFeedCall.cancel();
    }
  }
}

