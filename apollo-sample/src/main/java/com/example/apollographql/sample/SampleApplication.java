package com.example.apollographql.sample;

import android.app.Application;

import com.apollographql.android.converter.pojo.ApolloConverterFactory;
import com.example.DroidDetails;
import com.example.Films;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.io.File;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class SampleApplication extends Application {
  private static final String BASE_URL = "http://graphql.org";
  private OkHttpClient okHttpClient;
  private Retrofit retrofit;
  private ApiService service;

  @Override public void onCreate() {
    super.onCreate();
    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
    okHttpClient = new OkHttpClient.Builder()
        .cache(new Cache(new File(getCacheDir(), "okhttp"), 10 * 1024))
        .addNetworkInterceptor(loggingInterceptor)
        .build();
    retrofit = new Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(new ApolloConverterFactory.Builder()
            .withResponseFieldMapper(DroidDetails.Data.class, new DroidDetails.Data.Mapper(DroidDetails.Data.FACTORY))
            .withResponseFieldMapper(Films.Data.class, new Films.Data.Mapper(Films.Data.FACTORY))
            .build())
        .addConverterFactory(MoshiConverterFactory.create())
        .build();
    service = retrofit.create(ApiService.class);
  }

  public OkHttpClient okHttpClient() {
    return okHttpClient;
  }

  public Retrofit retrofit() {
    return retrofit;
  }

  public ApiService service() {
    return service;
  }
}
