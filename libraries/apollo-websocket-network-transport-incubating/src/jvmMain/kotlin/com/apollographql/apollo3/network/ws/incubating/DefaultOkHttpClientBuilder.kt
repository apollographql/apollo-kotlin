package com.apollographql.apollo3.network.ws.incubating


import okhttp3.OkHttpClient

internal val defaultOkHttpClientBuilder: OkHttpClient.Builder by lazy {
  OkHttpClient.Builder()
}
