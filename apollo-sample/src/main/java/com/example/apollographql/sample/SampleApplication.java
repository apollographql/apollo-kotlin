package com.example.apollographql.sample;

import android.app.Application;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper;
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class SampleApplication extends Application {

  private static final String BASE_URL = "http://10.0.2.2:3010/graphql";
  private ApolloClient apolloClient;

  @Override public void onCreate() {
    super.onCreate();
    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .addNetworkInterceptor(loggingInterceptor)
        .addNetworkInterceptor(new Interceptor() {
          @Override public Response intercept(Chain chain) throws IOException {
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            return chain.proceed(chain.request());
          }
        })
        .build();

    ApolloSqlHelper apolloSqlHelper = new ApolloSqlHelper(this, "githuntdb");
    NormalizedCacheFactory normalizedCache = new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION,
        new SqlNormalizedCacheFactory(apolloSqlHelper));

    apolloClient = ApolloClient.<ApolloCall>builder()
        .serverUrl(BASE_URL)
        .okHttpClient(okHttpClient)
        .normalizedCache(normalizedCache,
            new CacheKeyResolver<Map<String, Object>>() {
              @Nonnull @Override public CacheKey resolve(@Nonnull Map<String, Object> objectSource) {
                if (objectSource.get("__typename").equals("User")) {
                  String userKey = objectSource.get("__typename") + "." + objectSource.get("login");
                  return CacheKey.from(userKey);
                }
                if (objectSource.containsKey("id")) {
                  String typeNameAndIDKey = objectSource.get("__typename") + "." + objectSource.get("id");
                  return CacheKey.from(typeNameAndIDKey);
                }
                return CacheKey.NO_KEY;
              }
            })
        .build();
  }

  public ApolloClient apolloClient() {
    return apolloClient;
  }

}
