package com.apollographql.apollo3.runtime.java.interceptor.internal;

import com.apollographql.apollo3.api.ExecutionContext;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class AutoPersistedQueryInfo implements ExecutionContext.Element {
  public boolean hit;

  public AutoPersistedQueryInfo(boolean hit) {
    this.hit = hit;
  }

  @NotNull @Override public Key<?> getKey() {
    return KEY;
  }

  static final ExecutionContext.Key<AutoPersistedQueryInfo> KEY = new ExecutionContext.Key<AutoPersistedQueryInfo>() {};

  @NotNull @Override public ExecutionContext minusKey(@NotNull ExecutionContext.Key<?> key) {
    return ExecutionContext.Element.DefaultImpls.minusKey(this, key);
  }

  @Nullable @Override public <E extends Element> E get(@NotNull ExecutionContext.Key<E> key) {
    return ExecutionContext.Element.DefaultImpls.get(this, key);
  }

  @Override public <R> R fold(R initial, @NotNull Function2<? super R, ? super Element, ? extends R> operation) {
    return ExecutionContext.Element.DefaultImpls.fold(this, initial, operation);
  }

  @NotNull @Override public ExecutionContext plus(@NotNull ExecutionContext context) {
    return ExecutionContext.Element.DefaultImpls.plus(this, context);
  }
}
