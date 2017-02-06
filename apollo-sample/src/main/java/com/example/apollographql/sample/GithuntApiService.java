package com.example.apollographql.sample;

import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.converter.pojo.OperationRequest;
import com.example.FeedQuery;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

interface GithuntApiService {
  @POST("graphql") Observable<Response<FeedQuery.Data>> githuntFeed(@Body OperationRequest<FeedQuery.Variables> query);
}
