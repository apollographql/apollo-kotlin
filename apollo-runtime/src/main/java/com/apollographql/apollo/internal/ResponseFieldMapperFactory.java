package com.apollographql.apollo.internal;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * @deprecated The mapper is no longer used and will be removed in a future version
 */
@Deprecated
public final class ResponseFieldMapperFactory {
  private final ConcurrentHashMap<String, ResponseFieldMapper> pool = new ConcurrentHashMap<>();

  @NotNull public ResponseFieldMapper create(@NotNull Operation operation) {
    checkNotNull(operation, "operation == null");
    String operationId = operation.operationId();
    ResponseFieldMapper mapper = pool.get(operationId);
    if (mapper != null) {
      return mapper;
    }
    pool.putIfAbsent(operationId, operation.responseFieldMapper());
    return pool.get(operationId);
  }
}
