package com.apollographql.apollo.api.internal;

import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class SimpleOperationResponseParser {
  private SimpleOperationResponseParser() {
  }

  @SuppressWarnings("unchecked")
  public static <D extends Operation.Data, W> Response<W> parse(@NotNull final Map<String, Object> response,
      @NotNull final Operation<D, W, ?> operation, @NotNull final ScalarTypeAdapters scalarTypeAdapters) {
    checkNotNull(response, "response == null");
    checkNotNull(operation, "operation == null");
    checkNotNull(scalarTypeAdapters, "scalarTypeAdapters == null");

    final D data;
    final Map<String, Object> responseData = (Map<String, Object>) response.get("data");
    if (responseData != null) {
      final SimpleResponseReader responseReader = new SimpleResponseReader(responseData, operation.variables(), scalarTypeAdapters);
      data = operation.responseFieldMapper().map(responseReader);
    } else {
      data = null;
    }

    final List<Error> errors;
    final List<Map<String, Object>> responseErrors = (List<Map<String, Object>>) response.get("errors");
    if (responseErrors != null) {
      errors = new ArrayList<>();
      for (Map<String, Object> errorPayload : responseErrors) {
        errors.add(readError(errorPayload));
      }
    } else {
      errors = null;
    }

    return Response.<W>builder(operation)
        .data(operation.wrapData(data))
        .errors(errors)
        .build();
  }

  @SuppressWarnings("unchecked")
  private static Error readError(final Map<String, Object> payload) {
    String message = null;
    final List<Error.Location> locations = new ArrayList<>();
    final Map<String, Object> customAttributes = new HashMap<>();
    for (Map.Entry<String, Object> entry : payload.entrySet()) {
      if ("message".equals(entry.getKey())) {
        Object value = entry.getValue();
        message = value != null ? value.toString() : null;
      } else if ("locations".equals(entry.getKey())) {
        List<Map<String, Object>> locationItems = (List<Map<String, Object>>) entry.getValue();
        if (locationItems != null) {
          for (Map<String, Object> item : locationItems) {
            locations.add(readErrorLocation(item));
          }
        }
      } else {
        if (entry.getValue() != null) {
          customAttributes.put(entry.getKey(), entry.getValue());
        }
      }
    }
    return new Error(message, locations, customAttributes);
  }

  private static Error.Location readErrorLocation(final Map<String, Object> data) {
    long line = -1;
    long column = -1;
    if (data != null) {
      for (Map.Entry<String, Object> entry : data.entrySet()) {
        if ("line".equals(entry.getKey())) {
          line = ((Number) entry.getValue()).longValue();
        } else if ("column".equals(entry.getKey())) {
          column = ((Number) entry.getValue()).longValue();
        }
      }
    }
    return new Error.Location(line, column);
  }
}
