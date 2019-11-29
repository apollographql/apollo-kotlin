package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.Function;
import com.apollographql.apollo.api.internal.Optional;

import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * A Factory used to construct an instance of a {@link NormalizedCache} configured with the custom scalar adapters set
 * in {@link com.apollographql.apollo.ApolloClient.Builder#addCustomTypeAdapter(ScalarType, CustomTypeAdapter)}.
 */
public abstract class NormalizedCacheFactory<T extends NormalizedCache> {
  private Optional<NormalizedCacheFactory> nextFactory = Optional.absent();

  /**
   * @param recordFieldAdapter A {@link RecordFieldJsonAdapter} configured with the custom scalar adapters set in {@link
   *                           com.apollographql.apollo.ApolloClient.Builder#addCustomTypeAdapter(ScalarType,
   *                           CustomTypeAdapter)}.
   * @return An implementation of {@link NormalizedCache}.
   */
  public abstract T create(RecordFieldJsonAdapter recordFieldAdapter);

  public final NormalizedCache createChain(final RecordFieldJsonAdapter recordFieldAdapter) {
    if (nextFactory.isPresent()) {
      return create(recordFieldAdapter)
          .chain(nextFactory.map(new Function<NormalizedCacheFactory, NormalizedCache>() {
            @NotNull @Override public NormalizedCache apply(@NotNull NormalizedCacheFactory factory) {
              return factory.createChain(recordFieldAdapter);
            }
          }).get());
    } else {
      return create(recordFieldAdapter);
    }
  }

  public final NormalizedCacheFactory<T> chain(@NotNull NormalizedCacheFactory factory) {
    checkNotNull(factory, "factory == null");

    NormalizedCacheFactory leafFactory = this;
    while (leafFactory.nextFactory.isPresent()) {
      leafFactory = (NormalizedCacheFactory) leafFactory.nextFactory.get();
    }
    leafFactory.nextFactory = Optional.of(factory);

    return this;
  }
}
