package com.apollographql.apollo.api;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

/** TODO */
public interface Operation<D extends Operation.Data, T, V extends Operation.Variables> {
  /** TODO */
  String queryDocument();

  /** TODO */
  V variables();

  ResponseFieldMapper<D> responseFieldMapper();

  T wrapData(D data);

  /** TODO */
  interface Data {
  }

  /** TODO */
  class Variables {
    protected Variables() {
    }

    @Nonnull protected Map<String, Object> valueMap() {
      return Collections.emptyMap();
    }
  }

  Variables EMPTY_VARIABLES = new Variables();
}
