package com.example.apollographql.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.apollographql.android.api.graphql.Response;
import com.example.AllPosts;
import com.example.Upvote;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";

  @Nonnull private final List<Disposable> disposables = new ArrayList<Disposable>();
  @Nonnull private final PostsAdapter postsAdapter = new PostsAdapter();

  @Nullable private TextView txtResponse;
  @Nullable private RecyclerView responses;

  private final Consumer<Response<AllPosts.Data>> onAllPostsData = new Consumer<Response<AllPosts.Data>>() {
    @Override public void accept(Response<AllPosts.Data> response) {
      final TextView txtResponse = MainActivity.this.txtResponse;
      if (txtResponse != null) {
        txtResponse.setVisibility(View.GONE);
      }

      final RecyclerView responses = MainActivity.this.responses;
      if (responses != null) {
        responses.setVisibility(View.VISIBLE);
      }

      postsAdapter.allPosts(response.data().posts());
    }
  };

  private final Consumer<Response<Upvote.Data>> onUpvoteData = new Consumer<Response<Upvote.Data>>() {
    @Override public void accept(Response<Upvote.Data> response) {
      postsAdapter.upvotePost(response.data().upvotePost());
    }
  };

  private final Consumer<Integer> onClickUpvote = new Consumer<Integer>() {
    @Override public void accept(Integer postId) {
      final Upvote.Variables variables = Upvote.Variables.builder()
          .postId(postId)
          .build();

      final SampleApplication application = (SampleApplication) getApplication();
      final Disposable upvoteDataDisposable = application.frontPageService()
          .upvote(new Upvote(variables))
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(onUpvoteData, onError);

      disposables.add(upvoteDataDisposable);
    }
  };

  private final Consumer<Throwable> onError = new Consumer<Throwable>() {
    @Override public void accept(Throwable e) {
      Log.e(TAG, "", e);
      Toast.makeText(MainActivity.this, "onError(): " + e.getMessage(), Toast.LENGTH_LONG)
          .show();
    }
  };

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    txtResponse = (TextView) findViewById(R.id.txt_response);
    responses = (RecyclerView) findViewById(R.id.responses);

    responses.setAdapter(postsAdapter);
    responses.setLayoutManager(new LinearLayoutManager(this));
  }

  @Override protected void onResume() {
    super.onResume();

    txtResponse.setVisibility(View.VISIBLE);
    responses.setVisibility(View.GONE);

    final Disposable upvoteClickDisposable = postsAdapter.getUpvoteObservable()
        .subscribe(onClickUpvote, onError);
    disposables.add(upvoteClickDisposable);

    final SampleApplication application = (SampleApplication) getApplication();
    final Disposable allPostsDataDisposable = application.frontPageService()
        .allPosts(new AllPosts())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onAllPostsData, onError);
    disposables.add(allPostsDataDisposable);
  }

  @Override protected void onPause() {
    for (final Disposable disposable : disposables) {
      if (!disposable.isDisposed()) {
        disposable.dispose();
      }
    }

    disposables.clear();

    super.onPause();
  }
}
