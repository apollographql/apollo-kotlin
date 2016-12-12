package com.apollostack.api;

import java.util.List;

public interface GraphQLQuery {

  String operationDefinition();

  List<String> fragmentDefinitions();
}
