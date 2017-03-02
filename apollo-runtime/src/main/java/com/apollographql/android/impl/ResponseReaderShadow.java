package com.apollographql.android.impl;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;

import java.util.List;
import java.util.Map;

interface ResponseReaderShadow {

  void willResolveRootQuery(Operation operation);

  void willResolve(Field field, Operation.Variables variables);

  void didResolve(Field field, Operation.Variables variables);

  void didParseScalar(Object value);

  void willParseObject(Map<String, Object> objectMap);

  void didParseObject(Map<String, Object> objectMap);

  void didParseList(List array);

  void willParseElement(int atIndex);

  void didParseElement(int atIndex);

  void didParseNull();
}
