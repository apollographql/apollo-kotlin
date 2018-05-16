package com.apollographql.apollo.response;

import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.field.MapFieldValueResolver;
import com.apollographql.apollo.internal.json.BufferedSourceJsonReader;
import com.apollographql.apollo.internal.json.ResponseJsonStreamReader;
import com.apollographql.apollo.internal.response.RealResponseReader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import okio.BufferedSource;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.internal.json.ApolloJsonReader.responseJsonStreamReader;

@SuppressWarnings("WeakerAccess")
public final class OperationResponseParser<D extends Operation.Data, W> {
  final Operation<D, W, ?> operation;
  final ResponseFieldMapper responseFieldMapper;
  final ScalarTypeAdapters scalarTypeAdapters;
  final ResponseNormalizer<Map<String, Object>> responseNormalizer;

  @SuppressWarnings("unchecked") public OperationResponseParser(Operation<D, W, ?> operation,
      ResponseFieldMapper responseFieldMapper, ScalarTypeAdapters scalarTypeAdapters) {
    this(operation, responseFieldMapper, scalarTypeAdapters, ResponseNormalizer.NO_OP_NORMALIZER);
  }

  public OperationResponseParser(Operation<D, W, ?> operation, ResponseFieldMapper responseFieldMapper,
      ScalarTypeAdapters scalarTypeAdapters, ResponseNormalizer<Map<String, Object>> responseNormalizer) {
    this.operation = operation;
    this.responseFieldMapper = responseFieldMapper;
    this.scalarTypeAdapters = scalarTypeAdapters;
    this.responseNormalizer = responseNormalizer;
  }

  @SuppressWarnings("unchecked")
  public Response<W> parse(@NotNull Map<String, Object> payload) {
    checkNotNull(payload, "payload == null");

    D data = null;
    if (payload.containsKey("data")) {
      Map<String, Object> buffer = (Map<String, Object>) payload.get("data");
      RealResponseReader<Map<String, Object>> realResponseReader = new RealResponseReader<>(operation.variables(),
          buffer, new MapFieldValueResolver(), scalarTypeAdapters, responseNormalizer);
      data = (D) responseFieldMapper.map(realResponseReader);
    }

    List<Error> errors = null;
    if (payload.containsKey("errors")) {
      List<Map<String, Object>> errorPayloads = (List<Map<String, Object>>) payload.get("errors");
      if (errorPayloads != null) {
        errors = new ArrayList<>();
        for (Map<String, Object> errorPayload : errorPayloads) {
          errors.add(readError(errorPayload));
        }
      }
    }

    return Response.<W>builder(operation)
        .data(operation.wrapData(data))
        .errors(errors)
        .dependentKeys(responseNormalizer.dependentKeys())
        .build();
  }

  public Response<W> parse(BufferedSource source) throws IOException {
    responseNormalizer.willResolveRootQuery(operation);
    BufferedSourceJsonReader jsonReader = null;
    try {
      jsonReader = new BufferedSourceJsonReader(source);
      jsonReader.beginObject();

      D data = null;
      List<Error> errors = null;
      ResponseJsonStreamReader responseStreamReader = responseJsonStreamReader(jsonReader);
      while (responseStreamReader.hasNext()) {
        String name = responseStreamReader.nextName();
        if ("data".equals(name)) {
          //noinspection unchecked
          data = (D) responseStreamReader.nextObject(true, new ResponseJsonStreamReader.ObjectReader<Object>() {
            @Override public Object read(ResponseJsonStreamReader reader) throws IOException {
              Map<String, Object> buffer = reader.toMap();
              RealResponseReader<Map<String, Object>> realResponseReader = new RealResponseReader<>(
                  operation.variables(), buffer, new MapFieldValueResolver(), scalarTypeAdapters, responseNormalizer);
              return responseFieldMapper.map(realResponseReader);
            }
          });
        } else if ("errors".equals(name)) {
          errors = readResponseErrors(responseStreamReader);
        } else {
          responseStreamReader.skipNext();
        }
      }
      jsonReader.endObject();
      return Response.<W>builder(operation)
          .data(operation.wrapData(data))
          .errors(errors)
          .dependentKeys(responseNormalizer.dependentKeys())
          .build();
    } finally {
      if (jsonReader != null) {
        jsonReader.close();
      }
    }
  }

  private List<Error> readResponseErrors(ResponseJsonStreamReader reader) throws IOException {
    return reader.nextList(true, new ResponseJsonStreamReader.ListReader<Error>() {
      @Override public Error read(ResponseJsonStreamReader reader) throws IOException {
        return reader.nextObject(true, new ResponseJsonStreamReader.ObjectReader<Error>() {
          @Override public Error read(ResponseJsonStreamReader reader) throws IOException {
            return readError(reader.toMap());
          }
        });
      }
    });
  }

  @SuppressWarnings("unchecked")
  private Error readError(Map<String, Object> payload) {
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

  @SuppressWarnings("ConstantConditions")
  private Error.Location readErrorLocation(Map<String, Object> data) {
    long line = -1;
    long column = -1;
    if (data != null) {
      for (Map.Entry<String, Object> entry : data.entrySet()) {
        if ("line".equals(entry.getKey())) {
          line = ((BigDecimal) entry.getValue()).longValue();
        } else if ("column".equals(entry.getKey())) {
          column = ((BigDecimal) entry.getValue()).longValue();
        }
      }
    }
    return new Error.Location(line, column);
  }
}
