package com.apollostack.api;

import java.util.List;

public interface GraphQLQuery<T> {

  String getOperationDefinition();

  List<String> getFragmentDefinitions();
}
