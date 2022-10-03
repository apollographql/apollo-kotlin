package com.apollographql.apollo3.runtime.java.interceptor;

public interface ApolloDisposable {
  void dispose();

  boolean isDisposed();

  void addListener(Listener listener);

  void removeListener(Listener listener);

  interface Listener {
    void onDisposed();
  }
}
