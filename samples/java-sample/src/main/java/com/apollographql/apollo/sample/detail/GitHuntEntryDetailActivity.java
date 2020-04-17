package com.apollographql.apollo.sample.detail;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.apollographql.apollo.sample.EntryDetailQuery;
import com.apollographql.apollo.sample.GitHuntApplication;
import com.apollographql.apollo.sample.R;
import com.apollographql.apollo.sample.RepoCommentAddedSubscription;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GitHuntEntryDetailActivity extends AppCompatActivity {

  static final String TAG = GitHuntEntryDetailActivity.class.getSimpleName();
  private static final String ARG_REPOSITORY_FULL_NAME = "arg_repo_full_name";

  private GitHuntApplication application;

  ViewGroup content;
  ProgressBar progressBar;
  TextView description;
  TextView name;
  TextView postedBy;
  RecyclerView commentsList;

  private String repoFullName;
  private final CommentsRecyclerViewAdapter commentsListViewAdapter = new CommentsRecyclerViewAdapter();
  private final CompositeDisposable disposables = new CompositeDisposable();

  public static Intent newIntent(Context context, String repositoryFullName) {
    Intent intent = new Intent(context, GitHuntEntryDetailActivity.class);
    intent.putExtra(ARG_REPOSITORY_FULL_NAME, repositoryFullName);
    return intent;
  }

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initializeUserInterface();
    fetchRepositoryDetails();
    subscribeRepoCommentAdded();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId() == android.R.id.home) {
          this.finish();
          return true;
      }
      return super.onOptionsItemSelected(item);
  }

    private void initializeUserInterface() {
        setContentView(R.layout.activity_repository_detail);
        repoFullName = getIntent().getStringExtra(ARG_REPOSITORY_FULL_NAME);
        application = (GitHuntApplication) getApplication();
        content = (ViewGroup) findViewById(R.id.content_holder);
        progressBar = (ProgressBar) findViewById(R.id.loading_bar);
        name = (TextView) findViewById(R.id.tv_repository_name);
        description = (TextView) findViewById(R.id.tv_repository_description);
        postedBy = (TextView) findViewById(R.id.tv_posted_by);
        commentsList = (RecyclerView) findViewById(R.id.comments);
        commentsList.setAdapter(commentsListViewAdapter);
        commentsList.addItemDecoration(new RecyclerView.ItemDecoration() {
            final int verticalSpaceHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                    getResources().getDisplayMetrics());

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.top = verticalSpaceHeight;
                    outRect.bottom = verticalSpaceHeight / 2;
                } else if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
                    outRect.top = verticalSpaceHeight / 2;
                    outRect.bottom = verticalSpaceHeight;
                } else {
                    outRect.top = verticalSpaceHeight / 2;
                    outRect.bottom = verticalSpaceHeight / 2;
                }
            }
        });
        commentsList.addItemDecoration(new DividerItemDecoration(commentsList.getContext(),
                ((LinearLayoutManager) commentsList.getLayoutManager()).getOrientation()));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

  void setEntryData(EntryDetailQuery.Data data) {
    content.setVisibility(View.VISIBLE);
    progressBar.setVisibility(View.GONE);
    final EntryDetailQuery.Entry entry = data.entry();
    if (entry != null) {
        final EntryDetailQuery.Repository repository = entry.repository();
        if (repository != null) {
            setRepositoryData(repository);
        } else {
            Log.w(TAG, "Repository information is null.");
        }
        final EntryDetailQuery.PostedBy entryAuthor = entry.postedBy();
        if (entryAuthor != null) {
            postedBy.setText(getResources().getString(R.string.posted_by, entryAuthor.login()));
        } else {
            Log.w(TAG, "PostedBy information is null.");
        }
        List<String> comments = new ArrayList<>();
        final List<EntryDetailQuery.Comment> entryComments = entry.comments();
        for (EntryDetailQuery.Comment comment : entryComments) {
            comments.add(comment.content());
        }
        commentsListViewAdapter.setItems(comments);
    }
  }

    private void setRepositoryData(EntryDetailQuery.Repository repository) {
        name.setText(repository.full_name());
        description.setText(repository.description());
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
              if (dataResponse.data() != null) {
                  setEntryData(dataResponse.data());
              }
          }

          @Override public void onError(Throwable e) {
            Log.e(TAG, e.getMessage(), e);
          }

          @Override public void onComplete() {

          }
        }));
  }

  private void subscribeRepoCommentAdded() {
    ApolloSubscriptionCall<RepoCommentAddedSubscription.Data> subscriptionCall = application.apolloClient()
        .subscribe(new RepoCommentAddedSubscription(repoFullName));

    disposables.add(Rx2Apollo.from(subscriptionCall)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeWith(
            new DisposableSubscriber<Response<RepoCommentAddedSubscription.Data>>() {
              @Override public void onNext(Response<RepoCommentAddedSubscription.Data> response) {
                  final RepoCommentAddedSubscription.Data data = response.data();
                  if (data != null) {
                      RepoCommentAddedSubscription.CommentAdded newComment = data.commentAdded();
                      if (newComment != null) {
                          commentsListViewAdapter.addItem(newComment.content());
                      } else {
                          Log.w(TAG, "Comment added subscription data is null.");
                      }
                      Toast.makeText(GitHuntEntryDetailActivity.this, "Subscription response received", Toast.LENGTH_SHORT)
                              .show();
                  }
              }

              @Override public void onError(Throwable e) {
                Log.e(TAG, e.getMessage(), e);
                Toast.makeText(GitHuntEntryDetailActivity.this, "Subscription failure", Toast.LENGTH_SHORT).show();
              }

              @Override public void onComplete() {
                Log.d(TAG, "Subscription exhausted");
                Toast.makeText(GitHuntEntryDetailActivity.this, "Subscription complete", Toast.LENGTH_SHORT).show();
              }
            }
        )
    );
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    disposables.dispose();
  }
}
