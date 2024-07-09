package com.apollographql.apollo.api.java;

public class Assertions {
  // A version of Objects.requireNonNull that allows a customized message
  public static <T> T checkNotNull(T value, String errorMessage) {
    if (value == null) {
      throw new NullPointerException(errorMessage);
    }

    return value;
  }
}
