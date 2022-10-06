package com.apollographql.apollo3.runtime.java.internal;

import com.apollographql.apollo3.runtime.java.ApolloDisposable;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultApolloDisposable implements ApolloDisposable {

  private AtomicBoolean isDisposed = new AtomicBoolean(false);
  private ArrayList<Listener> listeners = new ArrayList<>();

  @Override public boolean isDisposed() {
    return isDisposed.get();
  }

  @Override public void addListener(Listener listener) {
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  @Override public void removeListener(Listener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  @Override public void dispose() {
    synchronized (listeners) {
      listeners.forEach(listener -> {
        listener.onDisposed();
      });
    }
    isDisposed.set(true);
  }
}
