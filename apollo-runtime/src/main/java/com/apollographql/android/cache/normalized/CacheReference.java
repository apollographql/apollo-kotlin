package com.apollographql.android.cache.normalized;

//Todo: provide serializable reference to differentiate between a reference and regular string
// (Issue: https://github.com/apollographql/apollo-android/issues/265)
public final class CacheReference {

  private final String key;

  public CacheReference(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CacheReference that = (CacheReference) o;

    return key != null ? key.equals(that.key) : that.key == null;

  }

  @Override public int hashCode() {
    return key != null ? key.hashCode() : 0;
  }

  @Override public String toString() {
    return key;
  }
}

