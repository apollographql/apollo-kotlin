package com.apollographql.apollo.internal.reader;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.internal.Optional;

import java.util.List;

public interface ResponseReaderShadow<R> {

  void willResolveRootQuery(Operation operation);

  void willResolve(Field field, Operation.Variables variables);

  void didResolve(Field field, Operation.Variables variables);

  void didParseScalar(Object value);

  void willParseObject(Field objectField, Optional<R> objectSource);

  void didParseObject(Field objectField, Optional<R> objectSource);

  void didParseList(List array);

  void willParseElement(int atIndex);

  void didParseElement(int atIndex);

  void didParseNull();
}
