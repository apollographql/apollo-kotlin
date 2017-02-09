package com.example.apollographql.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.apollographql.android.api.graphql.Response;
import com.example.AllPosts;

import fragment.PostDetails;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    final TextView txtResponse = (TextView) findViewById(R.id.txt_response);
    SampleApplication application = (SampleApplication) getApplication();
    application.postsService()
        .allPosts(new AllPosts())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<Response<AllPosts.Data>>() {
          @Override public void onSubscribe(Disposable d) {
          }

          @Override public void onNext(Response<AllPosts.Data> response) {
            final StringBuilder builder = new StringBuilder();
            for (AllPosts.Data.Post post : response.data().posts()) {
              final PostDetails details = post.fragments().postDetails();
              final PostDetails.Author author = details.author();
              builder.append("title: ").append(details.title());
              if (author != null) {
                builder.append(" author.firstName: ").append(author.firstName());
                builder.append(" author.lastName: ").append(author.lastName());
              }
              builder.append("\n~~~~~~~~~~~\n\n");
            }
            txtResponse.setText(builder.toString());
          }

          @Override public void onError(Throwable e) {
            Log.e(TAG, "", e);
            Toast.makeText(MainActivity.this, "onError(): " + e.getMessage(), Toast.LENGTH_LONG)
                .show();
          }

          @Override public void onComplete() {
          }
        });
  }


}
