package com.apollographql.apollo.internal.reader;

import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.internal.Optional;

import java.util.List;

public interface ResponseReaderShadow<R> {

  void willResolveRootQuery(Operation operation);

  void willResolve(ResponseField field, Operation.Variables variables);

  void didResolve(ResponseField field, Operation.Variables variables);

  void didParseScalar(Object value);

  void willParseObject(ResponseField objectField, Optional<R> objectSource);

  void didParseObject(ResponseField objectField, Optional<R> objectSource);

  void didParseList(List array);

  void willParseElement(int atIndex);

  void didParseElement(int atIndex);

  void didParseNull();
}
