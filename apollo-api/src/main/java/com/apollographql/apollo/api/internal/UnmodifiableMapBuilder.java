package com.apollographql.apollo.api.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UnmodifiableMapBuilder<K, V> {

  private final Map<K, V> map;

  public UnmodifiableMapBuilder(int initialCapacity) {
    this.map = new HashMap<>(initialCapacity);
  }

  public UnmodifiableMapBuilder put(K key, V value) {
    map.put(key, value);
    return this;
  }

  public Map<K, V> build() {
    return Collections.unmodifiableMap(map);
  }

}
