package com.apollographql.android.cache.normalized;

import java.util.Collection;

import javax.annotation.Nonnull;

public interface ReadTransaction {

  Record read(@Nonnull String key);

  Collection<Record> read(@Nonnull Collection<String> keys);

  void finishRead();

}
