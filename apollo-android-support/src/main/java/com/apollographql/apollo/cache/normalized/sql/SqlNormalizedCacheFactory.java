package com.apollographql.apollo.cache.normalized.sql;

import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class SqlNormalizedCacheFactory extends NormalizedCacheFactory<SqlNormalizedCache> {
  private final ApolloSqlHelper helper;

  public SqlNormalizedCacheFactory(ApolloSqlHelper helper) {
    this.helper = checkNotNull(helper, "helper == null");
  }

  @Override public SqlNormalizedCache create(RecordFieldJsonAdapter recordFieldAdapter) {
    return new SqlNormalizedCache(recordFieldAdapter, helper);
  }
}
