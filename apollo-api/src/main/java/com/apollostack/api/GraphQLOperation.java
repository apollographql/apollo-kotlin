package com.apollostack.api;

/** TODO */
public interface GraphQLOperation<V extends GraphQLOperation.Variables> {
  /** TODO */
  String queryDocument();

  /** TODO */
  V variables();

  /** TODO */
  interface Data {
  }

  /** TODO */
  abstract class Variables {
    protected Variables() {
    }
  }

  Variables EMPTY_VARIABLES = new Variables() {
  };
}
