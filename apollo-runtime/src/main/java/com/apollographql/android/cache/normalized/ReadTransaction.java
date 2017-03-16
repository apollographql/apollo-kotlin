package com.apollographql.android.cache.normalized;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ReadTransaction {

  @Nullable Record read(@Nonnull String key);

  Collection<Record> read(@Nonnull Collection<String> keys);

  void close();
}
