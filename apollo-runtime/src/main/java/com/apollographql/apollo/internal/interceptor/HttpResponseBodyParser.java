package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.field.MapFieldValueResolver;
import com.apollographql.apollo.internal.json.BufferedSourceJsonReader;
import com.apollographql.apollo.internal.json.ResponseJsonStreamReader;
import com.apollographql.apollo.internal.reader.RealResponseReader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;

import static com.apollographql.apollo.internal.json.ApolloJsonReader.responseJsonStreamReader;

final class HttpResponseBodyParser<D extends Operation.Data, W> {
  private final Operation<D, W, ?> operation;
  private final ResponseFieldMapper responseFieldMapper;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;

  HttpResponseBodyParser(Operation<D, W, ?> operation, ResponseFieldMapper responseFieldMapper,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
    this.operation = operation;
    this.responseFieldMapper = responseFieldMapper;
    this.customTypeAdapters = customTypeAdapters;
  }

  public Response<W> parse(ResponseBody responseBody,
      final ResponseNormalizer<Map<String, Object>> networkResponseNormalizer) throws IOException {
    networkResponseNormalizer.willResolveRootQuery(operation);
    BufferedSourceJsonReader jsonReader = null;
    try {
      jsonReader = new BufferedSourceJsonReader(responseBody.source());
      jsonReader.beginObject();

      ResponseJsonStreamReader responseStreamReader = responseJsonStreamReader(jsonReader);
      D data = null;
      List<Error> errors = null;
      while (responseStreamReader.hasNext()) {
        String name = responseStreamReader.nextName();
        if ("data".equals(name)) {
          //noinspection unchecked
          data = (D) responseStreamReader.nextObject(true, new ResponseJsonStreamReader.ObjectReader<Object>() {
            @Override public Object read(ResponseJsonStreamReader reader) throws IOException {
              Map<String, Object> buffer = reader.toMap();
              RealResponseReader<Map<String, Object>> realResponseReader = new RealResponseReader<>(
                  operation.variables(), buffer, new MapFieldValueResolver(), customTypeAdapters,
                  networkResponseNormalizer);
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
          .dependentKeys(networkResponseNormalizer.dependentKeys())
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
            return readError(reader);
          }
        });
      }
    });
  }

  @SuppressWarnings("unchecked") private Error readError(ResponseJsonStreamReader reader) throws IOException {
    String message = null;
    final List<Error.Location> locations = new ArrayList<>();
    final Map<String, Object> customAttributes = new HashMap<>();
    for (Map.Entry<String, Object> entry : reader.toMap().entrySet()) {
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
  private Error.Location readErrorLocation(Map<String, Object> data) throws IOException {
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
