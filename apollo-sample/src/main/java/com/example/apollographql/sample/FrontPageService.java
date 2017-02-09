package com.example.apollographql.sample;

import com.apollographql.android.api.graphql.Response;
import com.example.AllPosts;
import com.example.Upvote;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

interface FrontPageService {
  @POST("graphql") Observable<Response<AllPosts.Data>> allPosts(@Body AllPosts query);
  @POST("graphql") Observable<Response<Upvote.Data>> upvote(@Body Upvote query);
}
