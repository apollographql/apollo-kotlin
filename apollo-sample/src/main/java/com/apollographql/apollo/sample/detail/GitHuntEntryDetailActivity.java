package com.apollographql.apollo.sample.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.apollographql.apollo.sample.EntryDetailQuery;
import com.apollographql.apollo.sample.GitHuntApplication;
import com.apollographql.apollo.sample.R;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class GitHuntEntryDetailActivity extends AppCompatActivity {

  private static final String TAG = GitHuntEntryDetailActivity.class.getSimpleName();
  private static final String ARG_REPOSITORY_FULL_NAME = "arg_repo_full_name";

  private GitHuntApplication application;

  ViewGroup content;
  ProgressBar progressBar;
  TextView description;
  TextView name;
  TextView postedBy;

  private String repoFullName;

  private CompositeDisposable disposables = new CompositeDisposable();

  public static Intent newIntent(Context context, String repositoryFullName) {
    Intent intent = new Intent(context, GitHuntEntryDetailActivity.class);
    intent.putExtra(ARG_REPOSITORY_FULL_NAME, repositoryFullName);
    return intent;
  }

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_repository_detail);
    repoFullName = getIntent().getStringExtra(ARG_REPOSITORY_FULL_NAME);
    application = (GitHuntApplication) getApplication();
    content = (ViewGroup) findViewById(R.id.content_holder);
    progressBar = (ProgressBar) findViewById(R.id.loading_bar);
    name = (TextView) findViewById(R.id.tv_repository_name);
    description = (TextView) findViewById(R.id.tv_repository_description);
    postedBy = (TextView) findViewById(R.id.tv_posted_by);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    fetchRepositoryDetails();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        this.finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void setEntryData(EntryDetailQuery.Data data) {
    content.setVisibility(View.VISIBLE);
    progressBar.setVisibility(View.GONE);

    final EntryDetailQuery.Entry entry = data.entry();
    if (entry != null) {
      name.setText(entry.repository().full_name());
      description.setText(entry.repository().description());
      postedBy.setText(getResources().getString(R.string.posted_by, entry.postedBy().login()));
    }
  }

  private void fetchRepositoryDetails() {
    ApolloCall<EntryDetailQuery.Data> entryDetailQuery = application.apolloClient()
        .query(new EntryDetailQuery(repoFullName))
        .responseFetcher(ApolloResponseFetchers.CACHE_FIRST);

    //Example call using Rx2Support
    disposables.add(Rx2Apollo.from(entryDetailQuery)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeWith(new DisposableObserver<Response<EntryDetailQuery.Data>>() {
          @Override public void onNext(Response<EntryDetailQuery.Data> dataResponse) {
            setEntryData(dataResponse.data());
          }

          @Override public void onError(Throwable e) {
            Log.e(TAG, e.getMessage(), e);
          }

          @Override public void onComplete() {

          }
        }));
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    disposables.dispose();
  }
}
