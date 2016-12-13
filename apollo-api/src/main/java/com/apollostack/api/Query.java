package com.apollostack.api;

import java.util.List;
import java.util.Map;

/** TODO */
public interface Query {
  /** TODO */
  String operationDefinition();
  /** TODO */
  List<String> fragmentDefinitions();
  /** TODO */
  Map<String, Object> variableDefinitions();
  /** TODO */
  interface Data {
  }
}
