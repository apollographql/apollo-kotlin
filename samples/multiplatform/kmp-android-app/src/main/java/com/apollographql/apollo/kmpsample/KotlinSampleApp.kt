package com.apollographql.apollo.kmpsample

import android.app.Application
import com.apollographql.apollo.ApolloClient
import okhttp3.OkHttpClient

@Suppress("unused")
class KotlinSampleApp : Application() {
  private val baseUrl = "https://api.github.com/graphql"

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
        .serverUrl(baseUrl)
        .okHttpClient(okHttpClient)
        .build()
  }
}
