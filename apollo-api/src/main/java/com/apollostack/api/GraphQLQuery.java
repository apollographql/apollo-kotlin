package com.apollostack.api;

import java.util.List;
import java.util.Map;

public interface GraphQLQuery {

  String operationDefinition();

  List<String> fragmentDefinitions();

  Map<String, Object> variableDefinitions();
}
