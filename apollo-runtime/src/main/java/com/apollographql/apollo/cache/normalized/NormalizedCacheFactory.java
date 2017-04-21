package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.ScalarType;

/**
 * A Factory used to construct an instance of a {@link NormalizedCache} configured with the custom scalar adapters set
 * in {@link com.apollographql.apollo.ApolloClient.Builder#addCustomTypeAdapter(ScalarType, CustomTypeAdapter)}.
 */
public interface NormalizedCacheFactory<T extends NormalizedCache> {

  /**
   * @param recordFieldAdapter A {@link RecordFieldAdapter} configured with the custom scalar adapters set in {@link
   *                           com.apollographql.apollo.ApolloClient.Builder#addCustomTypeAdapter(ScalarType,
   *                           CustomTypeAdapter)}.
   * @return An implementation of {@link NormalizedCache}.
   */
  T createNormalizedCache(RecordFieldAdapter recordFieldAdapter);

}
