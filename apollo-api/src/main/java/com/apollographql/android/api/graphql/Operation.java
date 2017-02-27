package com.apollographql.android.api.graphql;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

/** TODO */
public interface Operation<T extends Operation.Data, V extends Operation.Variables> {
  /** TODO */
  String queryDocument();

  /** TODO */
  V variables();

  ResponseFieldMapper<? extends Operation.Data> responseFieldMapper();

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
