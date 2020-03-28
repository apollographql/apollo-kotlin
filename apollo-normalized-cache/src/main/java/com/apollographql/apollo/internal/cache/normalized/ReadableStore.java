package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.Record;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface ReadableStore {

  @Nullable Record read(@NotNull String key, @NotNull CacheHeaders cacheHeaders);

  Collection<Record> read(@NotNull Collection<String> keys, @NotNull CacheHeaders cacheHeaders);

}
