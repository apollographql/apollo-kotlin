package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.Record;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ReadableStore {

  @Nullable Record read(@Nonnull String key, @Nonnull CacheHeaders cacheHeaders);

  Collection<Record> read(@Nonnull Collection<String> keys, @Nonnull CacheHeaders cacheHeaders);

}
