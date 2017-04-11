package com.apollographql.apollo.cache.normalized.sql;

import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.RecordFieldAdapter;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class SqlNormalizedCacheFactory implements NormalizedCacheFactory<SqlNormalizedCache> {

  private final ApolloSqlHelper helper;

  public SqlNormalizedCacheFactory(ApolloSqlHelper helper) {
    this.helper = checkNotNull(helper, "helper == null");
  }

  @Override public SqlNormalizedCache createNormalizedCache(RecordFieldAdapter recordFieldAdapter) {
    return new SqlNormalizedCache(recordFieldAdapter, helper);
  }
}
