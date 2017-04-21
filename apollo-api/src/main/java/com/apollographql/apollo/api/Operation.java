package com.apollographql.apollo.api;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Abstraction for a GraphQL operation (mutation or query)
 */
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

  /**
   * Abstraction for the variables which are a part of the GraphQL query.
   */
  class Variables {
    protected Variables() {
    }

    @Nonnull protected Map<String, Object> valueMap() {
      return Collections.emptyMap();
    }
  }

  Variables EMPTY_VARIABLES = new Variables();
}
