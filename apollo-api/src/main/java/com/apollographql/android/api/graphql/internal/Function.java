package com.apollographql.android.api.graphql.internal;

import javax.annotation.Nonnull;

public interface Function<T, R> {
  /**
   * Apply some calculation to the input value and return some other value.
   *
   * @param t the input value
   * @return the output value
   */
  @Nonnull
  R apply(@Nonnull T t);
}
