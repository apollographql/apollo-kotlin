package com.apollographql.apollo.api.internal;

import org.jetbrains.annotations.NotNull;

public interface Function<T, R> {
  /**
   * Apply some calculation to the input value and return some other value.
   *
   * @param t the input value
   * @return the output value
   */
  @NotNull
  R apply(@NotNull T t);
}
