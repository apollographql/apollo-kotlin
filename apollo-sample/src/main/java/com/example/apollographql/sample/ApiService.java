package com.example.apollographql.sample;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.converter.pojo.OperationRequest;
import com.example.DroidDetails;
import com.example.Films;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

interface ApiService {
  @POST("/") Observable<Response<DroidDetails.Data>> droidDetails(
      @Body OperationRequest<Operation.Variables> query);
  @POST("/") Observable<Response<Films.Data>> allFilms(
      @Body OperationRequest<Operation.Variables> query);
}
