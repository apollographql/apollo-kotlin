package com.apollographql.apollo.internal;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseFieldMapper;

import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class ResponseFieldMapperFactory {
  private final ConcurrentHashMap<Class, ResponseFieldMapper> pool = new ConcurrentHashMap<>();

  @NotNull public ResponseFieldMapper create(@NotNull Operation operation) {
    checkNotNull(operation, "operation == null");
    Class operationClass = operation.getClass();
    ResponseFieldMapper mapper = pool.get(operationClass);
    if (mapper != null) {
      return mapper;
    }
    pool.putIfAbsent(operationClass, operation.responseFieldMapper());
    return pool.get(operationClass);
  }
}
