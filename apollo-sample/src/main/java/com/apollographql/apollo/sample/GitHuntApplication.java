package com.apollographql.apollo.sample;

import android.app.Application;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper;
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory;

import java.util.Map;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;

public class GitHuntApplication extends Application {

  private static final String BASE_URL = "https://githunt-api.herokuapp.com/graphql";
  private static final String SQL_CACHE_NAME = "githuntdb";
  private ApolloClient apolloClient;

  @Override public void onCreate() {
    super.onCreate();
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .build();

    ApolloSqlHelper apolloSqlHelper = new ApolloSqlHelper(this, SQL_CACHE_NAME);
    NormalizedCacheFactory normalizedCacheFactory = new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION,
        new SqlNormalizedCacheFactory(apolloSqlHelper));

    CacheKeyResolver<Map<String, Object>> cacheKeyResolver = new CacheKeyResolver<Map<String, Object>>() {
      @Nonnull @Override public CacheKey resolve(@Nonnull Map<String, Object> objectSource) {
        //Specific id for User type.
        if ("User".equals(objectSource.get("__typename"))) {
          String userKey = objectSource.get("__typename") + "." + objectSource.get("login");
          return CacheKey.from(userKey);
        }
        //Use id as default case.
        if (objectSource.containsKey("id")) {
          String typeNameAndIDKey = objectSource.get("__typename") + "." + objectSource.get("id");
          return CacheKey.from(typeNameAndIDKey);
        }
        return CacheKey.NO_KEY;
      }
    };

    apolloClient = ApolloClient.builder()
        .serverUrl(BASE_URL)
        .okHttpClient(okHttpClient)
        .normalizedCache(normalizedCacheFactory, cacheKeyResolver)
        .build();
  }

  public ApolloClient apolloClient() {
    return apolloClient;
  }

}
