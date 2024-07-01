package com.apollographql.apollo.runtime.java;

public interface ApolloDisposable {
  void dispose();

  boolean isDisposed();

  void addListener(Listener listener);

  void removeListener(Listener listener);

  interface Listener {
    void onDisposed();
  }
}
