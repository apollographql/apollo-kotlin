package com.apollographql.apollo.kmpsample

import android.app.Application
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.kmpsample.data.BASE_URL
import okhttp3.OkHttpClient

class KotlinSampleApp : Application() {

  val apolloClient: ApolloClient by lazy {
    val okHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor { chain ->
          val request = chain.request().newBuilder()
              .addHeader("Authorization", "bearer ${BuildConfig.GITHUB_OAUTH_TOKEN}")
              .build()

          chain.proceed(request)
        }
        .build()

    ApolloClient.builder()
        .serverUrl(BASE_URL)
        .okHttpClient(okHttpClient)
        .build()
  }
}
