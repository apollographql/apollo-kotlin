package com.apollographql.apollo.api;

import javax.annotation.Nullable;

/**
 * An immutable object that wraps reference to another object. Reference can be in two states: <i>defined</i> means
 * reference to another object is set explicitly and <i>undefined</i> means reference is not set. <br/> It provides a
 * convenience way to distinguish the case when value is provided explicitly and should be serialized (even if it's
 * null) and the case when value is undefined (means it won't be serialized).
 *
 * @param <V> the type of instance that can be contained
 */
public final class Input<V> {
  @Nullable public final V value;
  public final boolean defined;

  private Input(V value, boolean defined) {
    this.value = value;
    this.defined = defined;
  }

  /**
   * Creates a new {@link Input} instance that is defined in case if {@code value} is not-null and undefined otherwise.
   *
   * @param value to be wrapped
   * @return a new {@link Input} instance
   */
  public static <V> Input<V> optional(@Nullable V value) {
    if (value == null) {
      return absent();
    } else {
      return fromNullable(value);
    }
  }

  /**
   * Creates a new {@link Input} instance that is always defined.
   *
   * @param value to be wrapped
   * @return a new {@link Input} instance
   */
  public static <V> Input<V> fromNullable(@Nullable V value) {
    return new Input<>(value, true);
  }

  /**
   * Creates a new {@link Input} instance that is always undefined.
   *
   * @param value to be wrapped
   * @return a new {@link Input} instance
   */
  public static <V> Input<V> absent() {
    return new Input<>(null, false);
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Input)) return false;

    Input<?> input = (Input<?>) o;

    return defined == input.defined && value != null && value.equals(input.value);
  }

  @Override public int hashCode() {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + (defined ? 1 : 0);
    return result;
  }
}
