package com.apollographql.apollo.api.internal;

import org.jetbrains.annotations.NotNull;

public interface Action<T> {
  void apply(@NotNull T t);
}
