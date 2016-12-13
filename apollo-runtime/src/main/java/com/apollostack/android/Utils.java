package com.apollostack.android;

import javax.annotation.Nullable;

final class Utils {
  private Utils() {
  }

  static <T> T checkNotNull(T reference, @Nullable Object errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return reference;
  }
}
