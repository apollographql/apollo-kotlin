package com.apollographql.apollo3.runtime.java.interceptor;

public interface ApolloDisposable {
  void dispose();

  boolean isDisposed();
}
