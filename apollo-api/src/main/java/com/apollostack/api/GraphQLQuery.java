package com.apollostack.api;

import java.util.List;

public interface GraphQLQuery {

  String getOperationDefinition();

  List<String> getFragmentDefinitions();
}
