package com.apollographql.apollo3.network


import okhttp3.OkHttpClient

internal val defaultOkHttpClientBuilder: OkHttpClient.Builder by lazy {
  OkHttpClient.Builder()
}
