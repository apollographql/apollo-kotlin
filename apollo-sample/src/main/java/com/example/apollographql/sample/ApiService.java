package com.example.apollographql.sample;

import com.apollographql.api.graphql.Response;
import com.example.DroidDetails;
import com.example.Films;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

interface ApiService {
  @POST("/") Observable<Response<DroidDetailsData>> droidDetails(@Body DroidDetails query);
  @POST("/") Observable<Response<FilmsData>> allFilms(@Body Films query);
}
