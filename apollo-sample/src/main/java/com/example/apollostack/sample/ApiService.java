package com.example.apollostack.sample;

import com.apollostack.api.graphql.Response;

import generatedIR.DroidDetails;
import generatedIR.Films;
import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

interface ApiService {
  @POST("/") Observable<Response<DroidDetailsData>> droidDetails(@Body DroidDetails query);
  @POST("/") Observable<Response<FilmsData>> allFilms(@Body Films query);
}