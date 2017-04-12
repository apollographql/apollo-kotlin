package com.example.apollographql.sample.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.apollographql.android.rx.RxApollo;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.example.EntryDetailQuery;
import com.example.EntryDetailQuery.Data.Entry;
import com.example.Upvote;
import com.example.apollographql.sample.R;
import com.example.apollographql.sample.SampleApplication;
import com.squareup.picasso.Picasso;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class GitHuntEntryDetailActivity extends AppCompatActivity {
  private static final String TAG = GitHuntEntryDetailActivity.class.getSimpleName();

  static final String ARG_REPOSITORY_FULL_NAME = "arg_repo_full_name";


  private SampleApplication application;

  ViewGroup content;
  ProgressBar progressBar;
  TextView description;
  TextView name;
  TextView postedBy;
  TextView voteCount;
  ImageView repoImage;
  ImageView postedByImage;
  Button upvote;

  String repoFullName;

  public static Intent newIntent(Context context, String repositoryFullName) {
    Intent intent = new Intent(context, GitHuntEntryDetailActivity.class);
    intent.putExtra(ARG_REPOSITORY_FULL_NAME, repositoryFullName);
    return intent;
  }

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_repository_detail);
    repoFullName = getIntent().getStringExtra(ARG_REPOSITORY_FULL_NAME);
    application = (SampleApplication) getApplication();
    content = (ViewGroup) findViewById(R.id.content_holder);
    progressBar = (ProgressBar) findViewById(R.id.loading_bar);
    name = (TextView) findViewById(R.id.tv_repository_name);
    description = (TextView) findViewById(R.id.tv_repository_description);
    repoImage = (ImageView) findViewById(R.id.iv_repository_icon);
    postedByImage = (ImageView) findViewById(R.id.iv_posted_by_icon);
    postedBy = (TextView) findViewById(R.id.tv_posted_by);
    voteCount = (TextView) findViewById(R.id.tv_upvote_counter);
    upvote = (Button) findViewById(R.id.upvote_button);

    upvote.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        upvote();
      }
    });

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    fetchRepositoryDetails();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        // do something useful
        return (true);
    }

    return (super.onOptionsItemSelected(item));
  }

  void setEntryData(EntryDetailQuery.Data data) {
    content.setVisibility(View.VISIBLE);
    progressBar.setVisibility(View.GONE);

    final Entry entry = data.entry();
    if (entry != null) {
      name.setText(entry.repository().full_name());
      description.setText(entry.repository().description());
      setUpvoteCount(entry.vote().vote_value());
    }

    Picasso.with(this).load(entry.repository().owner().avatar_url()).into(repoImage);
    postedBy.setText(entry.postedBy().login());
    Picasso.with(this).load(entry.postedBy().avatar_url()).into(postedByImage);
  }

  void setUpvoteCount(int upvoteCount) {
    voteCount.setText("Upvotes: " + upvoteCount);
  }

  void upvote() {
    upvote.setVisibility(View.GONE);
    ApolloCall<Upvote.Data> upvoteMutation = application.apolloClient()
        .newCall(new Upvote(repoFullName));
    RxApollo.from(upvoteMutation)
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<Upvote.Data>() {
          @Override public void onCompleted() {

          }

          @Override public void onError(Throwable e) {
            setUpvoteCount(1);
          }

          @Override public void onNext(Upvote.Data data) {
            setUpvoteCount(data.vote().vote().vote_value());
          }
        });
    setUpvoteCount(1);
  }

  //Example using RxWrapper
  void fetchRepositoryDetails() {
    ApolloCall<EntryDetailQuery.Data> entryDetailQuery = application.apolloClient()
        .newCall(new EntryDetailQuery(repoFullName))
        .cacheControl(CacheControl.CACHE_FIRST);
    RxApollo.from(entryDetailQuery)
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<EntryDetailQuery.Data>() {
          @Override public void onCompleted() {

          }

          @Override public void onError(Throwable e) {
            Log.e(TAG, e.getMessage(), e);
          }

          @Override public void onNext(EntryDetailQuery.Data data) {
            setEntryData(data);
          }
        });
  }

}
