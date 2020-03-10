/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apollographql.apollo.api.internal;


import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * Implementation of an {@link Optional} containing a reference.
 */

final class Present<T> extends Optional<T> {
  private final T reference;

  Present(T reference) {
    this.reference = reference;
  }

  @Override public boolean isPresent() {
    return true;
  }

  @Override public T get() {
    return reference;
  }

  @Override public T or(T defaultValue) {
    checkNotNull(defaultValue, "use Optional.orNull() instead of Optional.or(null)");
    return reference;
  }

  @Override public Optional<T> or(Optional<? extends T> secondChoice) {
    checkNotNull(secondChoice);
    return this;
  }

  @Override public <V> Optional<V> transform(Function<? super T, V> function) {
    return new Present<V>(checkNotNull(function.apply(reference),
        "the Function passed to Optional.transform() must not return null."));
  }

  @Override public <V> Optional<V> map(Function<? super T, V> function) {
    return new Present<V>(checkNotNull(function.apply(reference),
        "the Function passed to Optional.map() must not return null."));
  }

  @Override public <V> Optional<V> flatMap(Function<? super T, Optional<V>> function) {
    checkNotNull(function);
    return checkNotNull(function.apply(reference),
        "the Function passed to Optional.flatMap() must not return null.");
  }

  @Override public Optional<T> apply(final Action<T> action) {
    checkNotNull(action);
    return map(new Function<T, T>() {
      @NotNull @Override public T apply(@NotNull T t) {
        action.apply(t);
        return t;
      }
    });
  }

  @Override public T orNull() {
    return reference;
  }

  @Override public Set<T> asSet() {
    return Collections.singleton(reference);
  }

  @Override public boolean equals(@Nullable Object object) {
    if (object instanceof Present) {
      Present<?> other = (Present<?>) object;
      return reference.equals(other.reference);
    }
    return false;
  }

  @Override public int hashCode() {
    return 0x598df91c + reference.hashCode();
  }

  @Override public String toString() {
    return "Optional.of(" + reference + ")";
  }

  private static final long serialVersionUID = 0;
}
