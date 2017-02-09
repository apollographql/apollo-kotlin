package com.example.apollographql.sample;

import android.app.Application;

import com.apollographql.android.converter.ApolloConverterFactory;
import com.example.AllPosts;
import com.example.Upvote;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.io.File;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

public class SampleApplication extends Application {
  // Change localhost to your machine's local IP address when running from a device
  private static final String BASE_URL = "http://127.0.0.1:8080";
  private OkHttpClient okHttpClient;
  private Retrofit retrofit;
  private PostsService postsService;

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
            .withResponseFieldMapper(AllPosts.Data.class, new AllPosts.Data.Mapper(AllPosts.Data.FACTORY))
            .withResponseFieldMapper(Upvote.Data.class, new Upvote.Data.Mapper(Upvote.Data.FACTORY))
            .build())
        .build();
    postsService = retrofit.create(PostsService.class);
  }

  public OkHttpClient okHttpClient() {
    return okHttpClient;
  }

  public Retrofit retrofit() {
    return retrofit;
  }

  public PostsService postsService() {
    return postsService;
  }
}
