package com.apollographql.apollo3.api.java;

public class Assertions {
  public static <T> T checkNotNull(T value, String errorMessage) {
    if (value == null) {
      throw new NullPointerException(errorMessage);
    }

    return value;
  }
}