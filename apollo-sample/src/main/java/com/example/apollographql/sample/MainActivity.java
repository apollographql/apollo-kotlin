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

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";

  private final PostsAdapter postsAdapter = new PostsAdapter();

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    final TextView txtResponse = (TextView) findViewById(R.id.txt_response);
    final RecyclerView responses = (RecyclerView) findViewById(R.id.responses);

    responses.setAdapter(postsAdapter);
    responses.setLayoutManager(new LinearLayoutManager(this));

    postsAdapter.getUpvoteObservable().subscribe(new Observer<Integer>() {
      @Override public void onSubscribe(Disposable d) {
      }

      @Override public void onNext(Integer postId) {
        upvotePost(postId);
      }

      @Override public void onError(Throwable e) {
        showErrorToast(e);
      }

      @Override public void onComplete() {
      }
    });

    final SampleApplication application = (SampleApplication) getApplication();
    application.frontPageService()
        .allPosts(new AllPosts())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<Response<AllPosts.Data>>() {
          @Override public void onSubscribe(Disposable d) {
          }

          @Override public void onError(Throwable e) {
            showErrorToast(e);
          }

          @Override public void onComplete() {
          }

          @Override public void onNext(Response<AllPosts.Data> response) {
            txtResponse.setVisibility(View.GONE);
            responses.setVisibility(View.VISIBLE);

            postsAdapter.allPosts(response.data().posts());
          }
        });
  }

  private void upvotePost(Integer postId) {
    final Upvote.Variables variables = Upvote.Variables.builder()
        .postId(postId)
        .build();

    final SampleApplication application = (SampleApplication) getApplication();
    application.frontPageService()
        .upvote(new Upvote(variables))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<Response<Upvote.Data>>() {
          @Override public void onSubscribe(Disposable d) {
          }

          @Override public void onNext(Response<Upvote.Data> response) {
            postsAdapter.upvotePost(response.data().upvotePost());
          }

          @Override public void onError(Throwable e) {
            showErrorToast(e);
          }

          @Override public void onComplete() {
          }
        });
  }

  private void showErrorToast(Throwable e) {
    Log.e(TAG, "", e);
    Toast.makeText(this, "onError(): " + e.getMessage(), Toast.LENGTH_LONG)
        .show();
  }
}
