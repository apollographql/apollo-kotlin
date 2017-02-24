package com.example.apollographql.sample;

import android.app.Application;

import com.apollographql.android.ApolloClient;

import java.io.File;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class SampleApplication extends Application {

  private static final String BASE_URL = "https://githunt-api.herokuapp.com/graphql";
  private ApolloClient apolloClient;

  @Override public void onCreate() {
    super.onCreate();
    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .cache(new Cache(new File(getCacheDir(), "okhttp"), 10 * 1024))
        .addNetworkInterceptor(loggingInterceptor)
        .build();

    apolloClient = ApolloClient.builder()
            .serverUrl(BASE_URL)
            .okHttpClient(okHttpClient)
            .build();
  }

  public ApolloClient apolloClient() {
    return apolloClient;
  }
}
