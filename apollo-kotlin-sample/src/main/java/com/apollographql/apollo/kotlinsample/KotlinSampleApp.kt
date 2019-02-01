package com.apollographql.apollo.kotlinsample

import android.app.Application
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
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

    val apolloSqlHelper = ApolloSqlHelper.create(this, "github_cache")
    val sqlNormalizedCacheFactory = SqlNormalizedCacheFactory(apolloSqlHelper)
    val cacheKeyResolver = object : CacheKeyResolver() {
      override fun fromFieldRecordSet(field: ResponseField, recordSet: MutableMap<String, Any>): CacheKey {
        return if (recordSet["__typename"] == "Repository") {
          CacheKey.from(recordSet["id"] as String)
        } else {
          CacheKey.NO_KEY
        }
      }

      override fun fromFieldArguments(field: ResponseField, variables: Operation.Variables): CacheKey {
        return CacheKey.NO_KEY
      }
    }

    ApolloClient.builder()
      .serverUrl(baseUrl)
      .normalizedCache(sqlNormalizedCacheFactory, cacheKeyResolver)
      .okHttpClient(okHttpClient)
      .build()
  }

}