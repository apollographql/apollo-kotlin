package com.apollostack.api.graphql;

/** TODO */
public interface Operation<V extends Operation.Variables> {
  /** TODO */
  String queryDocument();

  /** TODO */
  V variables();

  /** TODO */
  interface Data {
  }

  /** TODO */
  class Variables {
    protected Variables() {
    }
  }

  Variables EMPTY_VARIABLES = new Variables();
}
