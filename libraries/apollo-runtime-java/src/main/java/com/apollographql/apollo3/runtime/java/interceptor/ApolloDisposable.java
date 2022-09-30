package com.apollographql.apollo3.runtime.java.interceptor;

public interface ApolloDisposable {
  void dispose();

  boolean isDisposed();

  void addListener(Listener listener);

  void removeCancellationListener(Listener listener);

  interface Listener {
    void onCancelled();
  }
}
