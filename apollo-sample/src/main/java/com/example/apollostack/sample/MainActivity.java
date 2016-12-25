package com.example.apollostack.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.Response;
import com.apollostack.converter.pojo.ApolloConverterFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import generatedIR.DroidDetails;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Converter;

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
        .subscribe(new Observer<Response<DroidDetailsData>>() {
          @Override public void onSubscribe(Disposable d) {
          }

          @Override public void onNext(Response<DroidDetailsData> response) {
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

    InputStream is = null;
    try {
      is = getAssets().open("TestQuery.json");
      ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json; charset=UTF-8"), toString(is));
      Converter<ResponseBody, Response<? extends Operation.Data>> converter = new ApolloConverterFactory()
          .responseBodyConverter(TestQueryWithFragmentResponseData.class, null, null);
      Response<TestQueryWithFragmentResponseData> response = (Response<TestQueryWithFragmentResponseData>) converter.convert(responseBody);
      System.out.println("MainActivity.onCreate: " + response);
    } catch (Exception e) {
      e.printStackTrace();
      if (is != null) {
        try {
          is.close();
        } catch (Exception e1) {
          // ignore
        }
      }
    }
  }

  private String toString(InputStream is) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    StringBuilder stringBuilder = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      stringBuilder.append(line).append('\n');
    }
    return stringBuilder.toString();
  }
}
