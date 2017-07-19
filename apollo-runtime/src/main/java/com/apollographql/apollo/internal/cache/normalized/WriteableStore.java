package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.Record;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;

public interface WriteableStore extends ReadableStore {

  Set<String> merge(@Nonnull Collection<Record> recordCollection, @Nonnull CacheHeaders cacheHeaders);
}
