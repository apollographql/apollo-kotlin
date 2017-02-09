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
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    final TextView txtResponse = (TextView) findViewById(R.id.txt_response);
    final RecyclerView responses = (RecyclerView) findViewById(R.id.responses);

    final SampleApplication application = (SampleApplication) getApplication();

    final Subject<Integer> upvoteObserver = PublishSubject.create();

    final PostsAdapter adapter = new PostsAdapter(upvoteObserver);
    responses.setAdapter(adapter);
    responses.setLayoutManager(new LinearLayoutManager(this));

    upvoteObserver.subscribe(new SimpleObserver<Integer>() {
      @Override public void onNext(Integer postId) {
        final Upvote.Variables variables = Upvote.Variables.builder()
            .postId(postId)
            .build();

        application.frontPageService()
            .upvote(new Upvote(variables))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleObserver<Response<Upvote.Data>>() {
              @Override public void onNext(Response<Upvote.Data> response) {
                adapter.upvotePost(response.data().upvotePost());
              }
            });
      }
    });

    application.frontPageService()
        .allPosts(new AllPosts())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new SimpleObserver<Response<AllPosts.Data>>() {
          @Override public void onNext(Response<AllPosts.Data> response) {
            txtResponse.setVisibility(View.GONE);
            responses.setVisibility(View.VISIBLE);

            adapter.allPosts(response.data().posts());
          }
        });
  }

  private abstract class SimpleObserver<T> implements Observer<T> {
    @Override public void onSubscribe(Disposable d) {
    }

    @Override public void onError(Throwable e) {
      Log.e(TAG, "", e);
      Toast.makeText(MainActivity.this, "onError(): " + e.getMessage(), Toast.LENGTH_LONG)
          .show();
    }

    @Override public void onComplete() {

    }
  }
}
