package com.example.apollographql.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.apollographql.android.api.graphql.Response;
import com.example.FeedQuery;
import com.example.type.FeedType;

import java.io.IOException;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";

  private SampleApplication application;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    final TextView txtResponse = (TextView) findViewById(R.id.txt_response);
    application = (SampleApplication) getApplication();

    Observable<Response<FeedQuery.Data>> query = Observable.fromCallable(new Callable<Response<FeedQuery.Data>>() {
      @Override public Response<FeedQuery.Data> call() throws Exception {
        return executeFeedQuery();
      }
    });

    query
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<Response<FeedQuery.Data>>() {
          @Override public void onSubscribe(Disposable d) {

          }

          @Override public void onNext(Response<FeedQuery.Data> dataResponse) {
            StringBuffer buffer = new StringBuffer();
            for (FeedQuery.Data.Feed feed : dataResponse.data().feed()) {
              buffer.append("name:" + feed.repository().fragments().repositoryFragment().name());
              buffer.append(" owner: " + feed.repository().fragments().repositoryFragment().owner().login());
              buffer.append(" postedBy: " + feed.postedBy().login());
              buffer.append("\n~~~~~~~~~~~");
              buffer.append("\n\n");
            }
            txtResponse.setText(buffer.toString());
          }

          @Override public void onError(Throwable e) {
            Log.e(TAG, e.getMessage(), e);
          }

          @Override public void onComplete() {

          }
        });
  }

  public Response<FeedQuery.Data> executeFeedQuery() throws IOException {
    return application.apolloClient().newCall(new FeedQuery(FeedQuery.Variables.builder()
        .limit(10)
        .type(FeedType.HOT)
        .build())).execute();
  }
}
