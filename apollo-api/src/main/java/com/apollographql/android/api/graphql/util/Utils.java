package com.apollographql.android.api.graphql.util;

import java.util.Set;

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

  public static <E> boolean areDisjoint(Set<E> setOne, Set<E> setTwo) {
    if (setOne == null || setTwo == null) {
      return true;
    }
    Set<E> smallerSet = setOne;
    Set<E> largerSet = setTwo;
    if (setOne.size() > setTwo.size()) {
      smallerSet = setTwo;
      largerSet = setOne;
    }
    for (E el : smallerSet) {
      if (largerSet.contains(el)) {
        return false;
      }
    }
    return true;
  }

  public static <T> T checkNotNull(T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }

}
