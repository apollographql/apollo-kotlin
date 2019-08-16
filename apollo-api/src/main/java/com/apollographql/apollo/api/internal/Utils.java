package com.apollographql.apollo.api.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contains utility methods for checking Preconditions
 */
public final class Utils {
  private Utils() {
  }

  /**
   * Checks if the object is null. Returns the object if it is not null, else throws a NullPointerException with the
   * error message.
   *
   * @param reference    the object whose nullability has to be checked
   * @param errorMessage the message to use with the NullPointerException
   * @param <T>          the value type
   * @return The object itself
   * @throws NullPointerException if the object is null
   */
  @NotNull public static <T> T checkNotNull(T reference, @Nullable Object errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return reference;
  }

  /**
   * Checks if the object is null. Returns the object if it is not null, else throws a NullPointerException.
   *
   * @param reference the object whose nullability has to be checked
   * @param <T>       the value type
   * @return The object itself
   * @throws NullPointerException if the object is null
   */
  @NotNull public static <T> T checkNotNull(T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }
}
