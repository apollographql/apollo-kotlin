package com.apollographql.apollo3.runtime.java;

import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;

import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultApolloDisposable implements ApolloDisposable {

  private AtomicBoolean isDisposed = new AtomicBoolean(false);

  @Override public boolean isDisposed() {
    return isDisposed.get();
  }
  @Override public void dispose() {
    isDisposed.set(true);
  }
}
