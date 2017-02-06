package com.example.apollographql.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.converter.pojo.OperationRequest;
import com.example.FeedQuery;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import type.FeedType;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    final TextView txtResponse = (TextView) findViewById(R.id.txt_response);
    SampleApplication application = (SampleApplication) getApplication();
    application.githuntApiService()
        .githuntFeed(new OperationRequest<>(
            new FeedQuery(FeedQuery.Variables.builder()
                .limit(10)
                .type(FeedType.HOT)
                .build())))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<Response<FeedQuery.Data>>() {
          @Override public void onSubscribe(Disposable d) {
          }

          @Override public void onNext(Response<FeedQuery.Data> response) {
            StringBuffer buffer = new StringBuffer();
            for (FeedQuery.Data.Feed feed : response.data().feed()) {
              buffer.append("name:" + feed.repository().fragments().repositoryFragment().name());
              buffer.append(" owner: " + feed.repository().fragments().repositoryFragment().owner().login());
              buffer.append(" postedBy: " + feed.postedBy().login());
              buffer.append("\n~~~~~~~~~~~");
              buffer.append("\n\n");
            }
            txtResponse.setText(buffer.toString());
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
