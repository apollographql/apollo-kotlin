package com.apollographql.apollo.internal.reader;

import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.internal.Optional;

import java.util.List;

public interface ResponseReaderShadow<R> {

  void willResolveRootQuery(Operation operation);

  void willResolve(ResponseField field, Operation.Variables variables);

  void didResolve(ResponseField field, Operation.Variables variables);

  void didResolveScalar(Object value);

  void willResolveObject(ResponseField objectField, Optional<R> objectSource);

  void didResolveObject(ResponseField objectField, Optional<R> objectSource);

  void didResolveList(List array);

  void willResolveElement(int atIndex);

  void didResolveElement(int atIndex);

  void didResolveNull();
}
