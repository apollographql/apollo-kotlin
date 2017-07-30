package com.apollographql.apollo.api.internal;

import javax.annotation.Nonnull;

public interface Action<T> {
  void apply(@Nonnull T t);
}
