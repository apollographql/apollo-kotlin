package com.apollographql.apollo.cache.normalized;

public class TestCustomScalar {
  public final String fieldOne;

  public TestCustomScalar(String fieldOne) {
    this.fieldOne = fieldOne;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TestCustomScalar that = (TestCustomScalar) o;

    return fieldOne != null ? fieldOne.equals(that.fieldOne) : that.fieldOne == null;

  }

  @Override public int hashCode() {
    return fieldOne != null ? fieldOne.hashCode() : 0;
  }
}
