package com.apollographql.apollo.api.internal;


import org.jetbrains.annotations.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public class Functions {
  /**
   * Returns the identity function.
   */
  // implementation is "fully variant"; E has become a "pass-through" type
  public static <E> Function<E, E> identity() {
    return (Function<E, E>) IdentityFunction.INSTANCE;
  }

  // enum singleton pattern
  private enum IdentityFunction implements Function<Object, Object> {
    INSTANCE;

    @Override
    @Nullable
    public Object apply(@Nullable Object o) {
      return o;
    }

    @Override
    public String toString() {
      return "Functions.identity()";
    }
  }

  /**
   * Returns a function that calls {@code toString()} on its argument. The function does not accept
   * nulls; it will throw a {@link NullPointerException} when applied to {@code null}.
   *
   * <p><b>Warning:</b> The returned function may not be <i>consistent with equals</i> (as
   * documented at {@link Function#apply}). For example, this function yields different results for
   * the two equal instances {@code ImmutableSet.of(1, 2)} and {@code ImmutableSet.of(2, 1)}.
   */
  public static Function<Object, String> toStringFunction() {
    return ToStringFunction.INSTANCE;
  }

  // enum singleton pattern
  private enum ToStringFunction implements Function<Object, String> {
    INSTANCE;

    @Override
    public String apply(Object o) {
      checkNotNull(o); // eager for GWT.
      return o.toString();
    }

    @Override
    public String toString() {
      return "Functions.toStringFunction()";
    }
  }
}
