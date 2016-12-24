package com.example.apollostack.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonReader;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.apollostack.android.ResponseJsonReader;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import generatedIR.DroidDetails;
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
        .droidDetails(new DroidDetails())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<com.apollostack.api.graphql.Response>() {
          @Override public void onSubscribe(Disposable d) {
          }

          @Override public void onNext(com.apollostack.api.graphql.Response response) {
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


    JsonReader jsonReader = null;
    try {
      InputStreamReader isr = new InputStreamReader(getAssets().open("TestQuery.json"));
      jsonReader = new JsonReader(new BufferedReader(isr));

      jsonReader.beginObject();
      ResponseJsonReader responseReader = new ResponseJsonReader(jsonReader);
      TestQueryResponse testQuery = new TestQueryResponse(responseReader);
      jsonReader.endObject();

      System.out.println("MainActivity.onCreate: " + testQuery);
    } catch (Exception e) {
      Log.e(TAG, e.toString(), e);
    } finally {
      if (jsonReader != null) {
        try {
          jsonReader.close();
        } catch (Exception e) {
          // ignore
        }
      }
    }
  }
}
