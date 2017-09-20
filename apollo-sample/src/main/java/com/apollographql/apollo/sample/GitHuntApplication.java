package com.apollographql.apollo.sample;

import android.app.Application;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
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

  private static final String BASE_URL = "https://api.githunt.com/graphql/";
  private static final String SQL_CACHE_NAME = "githuntdb";
  private ApolloClient apolloClient;

  @Override public void onCreate() {
    super.onCreate();
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .build();

    ApolloSqlHelper apolloSqlHelper = new ApolloSqlHelper(this, SQL_CACHE_NAME);
    NormalizedCacheFactory normalizedCacheFactory = new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
        .chain(new SqlNormalizedCacheFactory(apolloSqlHelper));

    CacheKeyResolver cacheKeyResolver = new CacheKeyResolver() {
      @Nonnull @Override
      public CacheKey fromFieldRecordSet(@Nonnull ResponseField field, @Nonnull Map<String, Object> recordSet) {
        String typeName = (String) recordSet.get("__typename");

        // Examples of custom keys based on type
        if ("User".equals(typeName)) {
          String userKey = typeName + "." + recordSet.get("login");
          return CacheKey.from(userKey);
        } else if ("Entry".equals(typeName)) {
          if (recordSet.containsKey("repository")) {
            String repoFullName = (String) ((Map<String, Object>) recordSet.get("repository")).get("full_name");
            return repoFullName != null ? CacheKey.from(repoFullName) : CacheKey.NO_KEY;
          }
          return CacheKey.NO_KEY;
        } else if ("Repository".equals(typeName)) {
          String repositoryName = (String) recordSet.get("name");
          return repositoryName != null ? CacheKey.from(repositoryName) : CacheKey.NO_KEY;
        } else if (recordSet.containsKey("id")) {
          String typeNameAndIDKey = recordSet.get("__typename") + "." + recordSet.get("id");
          return CacheKey.from(typeNameAndIDKey);
        }
        return CacheKey.NO_KEY;
      }

      @Nonnull @Override
      public CacheKey fromFieldArguments(@Nonnull ResponseField field, @Nonnull Operation.Variables variables) {
        if ("entry".equals(field.fieldName())) {
          String repoFullName = (String) variables.valueMap().get("repoFullName");
          return repoFullName != null ? CacheKey.from(repoFullName) : CacheKey.NO_KEY;
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
