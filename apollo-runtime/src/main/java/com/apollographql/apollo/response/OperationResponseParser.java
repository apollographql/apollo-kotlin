package com.apollographql.apollo.response;

import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.CustomScalarAdapters;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader;
import com.apollographql.apollo.api.internal.json.ResponseJsonStreamReader;
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer;
import com.apollographql.apollo.internal.field.MapFieldValueResolver;
import com.apollographql.apollo.api.internal.RealResponseReader;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

@SuppressWarnings("WeakerAccess")
public final class OperationResponseParser<D extends Operation.Data> {
  final Operation<D, ?> operation;
  final ResponseFieldMapper responseFieldMapper;
  final CustomScalarAdapters customScalarAdapters;
  final ResponseNormalizer<Map<String, Object>> responseNormalizer;

  @SuppressWarnings("unchecked") public OperationResponseParser(Operation<D, ?> operation,
      ResponseFieldMapper responseFieldMapper, CustomScalarAdapters customScalarAdapters) {
    this(operation, responseFieldMapper, customScalarAdapters, (ResponseNormalizer<Map<String, Object>>) ResponseNormalizer.NO_OP_NORMALIZER);
  }

  public OperationResponseParser(Operation<D, ?> operation, ResponseFieldMapper responseFieldMapper,
      CustomScalarAdapters customScalarAdapters, ResponseNormalizer<Map<String, Object>> responseNormalizer) {
    this.operation = operation;
    this.responseFieldMapper = responseFieldMapper;
    this.customScalarAdapters = customScalarAdapters;
    this.responseNormalizer = responseNormalizer;
  }

  @SuppressWarnings("unchecked")
  public Response<D> parse(@NotNull Map<String, Object> payload) {
    checkNotNull(payload, "payload == null");

    responseNormalizer.willResolveRootQuery(operation);

    D data = null;
    Map<String, Object> buffer = (Map<String, Object>) payload.get("data");
    if (buffer != null) {
      RealResponseReader<Map<String, Object>> realResponseReader = new RealResponseReader<>(operation.variables(),
          buffer, new MapFieldValueResolver(), customScalarAdapters, responseNormalizer);
      data = (D) responseFieldMapper.map(realResponseReader);
    }

    List<Error> errors = null;
    if (payload.containsKey("errors")) {
      List<Map<String, Object>> errorPayloads = (List<Map<String, Object>>) payload.get("errors");
      if (errorPayloads != null) {
        errors = new ArrayList<>();
        for (Map<String, Object> errorPayload : errorPayloads) {
          errors.add(parseError(errorPayload));
        }
      }
    }

    return Response.<D>builder(operation)
        .data(data)
        .errors(errors)
        .dependentKeys(responseNormalizer.dependentKeys())
        .extensions((Map<String, Object>) payload.get("extensions"))
        .build();
  }

  public Response<D> parse(BufferedSource source) throws IOException {
    responseNormalizer.willResolveRootQuery(operation);
    BufferedSourceJsonReader jsonReader = null;
    try {
      jsonReader = new BufferedSourceJsonReader(source);
      jsonReader.beginObject();

      D data = null;
      List<Error> errors = null;
      Map<String, Object> extensions = null;
      ResponseJsonStreamReader responseStreamReader = new ResponseJsonStreamReader(jsonReader);
      while (responseStreamReader.hasNext()) {
        String name = responseStreamReader.nextName();
        if ("data".equals(name)) {
          //noinspection unchecked
          data = (D) responseStreamReader.nextObject(true, new ResponseJsonStreamReader.ObjectReader<Object>() {
            @Override public Object read(ResponseJsonStreamReader reader) throws IOException {
              Map<String, Object> buffer = reader.toMap();
              RealResponseReader<Map<String, Object>> realResponseReader = new RealResponseReader<>(
                  operation.variables(), buffer, new MapFieldValueResolver(), customScalarAdapters, responseNormalizer);
              return responseFieldMapper.map(realResponseReader);
            }
          });
        } else if ("errors".equals(name)) {
          errors = readResponseErrors(responseStreamReader);
        } else if ("extensions".equals(name)) {
          extensions = responseStreamReader.nextObject(true, new ResponseJsonStreamReader.ObjectReader<Map<String, Object>>() {
            @Override public Map<String, Object> read(ResponseJsonStreamReader reader) throws IOException {
              return reader.toMap();
            }
          });
        } else {
          responseStreamReader.skipNext();
        }
      }
      jsonReader.endObject();
      return Response.<D>builder(operation)
          .data(data)
          .errors(errors)
          .dependentKeys(responseNormalizer.dependentKeys())
          .extensions(extensions)
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
            return parseError(reader.toMap());
          }
        });
      }
    });
  }

  @SuppressWarnings("unchecked")
  public static Error parseError(Map<String, Object> payload) {
    String message = "";
    final List<Error.Location> locations = new ArrayList<>();
    final Map<String, Object> customAttributes = new HashMap<>();
    for (Map.Entry<String, Object> entry : payload.entrySet()) {
      if ("message".equals(entry.getKey())) {
        Object value = entry.getValue();
        message = value != null ? value.toString() : "";
      } else if ("locations".equals(entry.getKey())) {
        List<Map<String, Object>> locationItems = (List<Map<String, Object>>) entry.getValue();
        if (locationItems != null) {
          for (Map<String, Object> item : locationItems) {
            locations.add(parseErrorLocation(item));
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
  private static Error.Location parseErrorLocation(Map<String, Object> data) {
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
