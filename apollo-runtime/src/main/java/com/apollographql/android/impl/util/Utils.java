package com.apollographql.android.impl.util;

import javax.annotation.Nullable;

public final class Utils {
  private Utils() {
  }

  public static <T> T checkNotNull(T reference, @Nullable Object errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return reference;
  }
}
