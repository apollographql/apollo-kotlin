package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.cache.normalized.Record;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ReadableCache {

  @Nullable Record read(@Nonnull String key);

  Collection<Record> read(@Nonnull Collection<String> keys);

}
