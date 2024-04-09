package com.apollographql.apollo3.network.websocket


import okhttp3.OkHttpClient

internal val defaultOkHttpClientBuilder: OkHttpClient.Builder by lazy {
  OkHttpClient.Builder()
}
