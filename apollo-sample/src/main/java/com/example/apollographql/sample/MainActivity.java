package com.example.apollographql.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.converter.pojo.OperationRequest;
import com.example.DroidDetails;

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
    application.service()
        .droidDetails(new OperationRequest<>(new DroidDetails()))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<Response<DroidDetails.Data>>() {
          @Override public void onSubscribe(Disposable d) {
          }

          @Override public void onNext(Response<DroidDetails.Data> response) {
            txtResponse.setText(response.data().toString());
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
