package com.apollographql.apollo.runtime.java.interceptor.internal;

import com.apollographql.apollo.api.ExecutionContext;
import org.jetbrains.annotations.NotNull;

class AutoPersistedQueryInfo implements ExecutionContext.Element {
  public boolean hit;

  public AutoPersistedQueryInfo(boolean hit) {
    this.hit = hit;
  }

  @NotNull @Override public Key<?> getKey() {
    return KEY;
  }

  static final ExecutionContext.Key<AutoPersistedQueryInfo> KEY = new ExecutionContext.Key<AutoPersistedQueryInfo>() {};
}
