package com.example.apollographql.sample;

import com.apollographql.android.api.graphql.Response;
import com.example.FeedQuery;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

interface GithuntApiService {
  @POST("graphql") Observable<Response<FeedQuery.Data>> githuntFeed(@Body FeedQuery query);
}
