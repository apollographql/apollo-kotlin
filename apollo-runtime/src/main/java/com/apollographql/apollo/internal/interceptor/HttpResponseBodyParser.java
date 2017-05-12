package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.field.MapFieldValueResolver;
import com.apollographql.apollo.internal.json.BufferedSourceJsonReader;
import com.apollographql.apollo.internal.reader.RealResponseReader;
import com.apollographql.apollo.internal.json.ResponseJsonStreamReader;

import java.io.IOException;
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
              Map<String, Object> buffer = reader.buffer();
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
      return new Response<>(operation, operation.wrapData(data), errors, networkResponseNormalizer.dependentKeys());
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

  private Error readError(ResponseJsonStreamReader reader) throws IOException {
    String message = null;
    List<Error.Location> locations = null;
    while (reader.hasNext()) {
      String name = reader.nextName();
      if ("message".equals(name)) {
        message = reader.nextString(false);
      } else if ("locations".equals(name)) {
        locations = reader.nextList(true, new ResponseJsonStreamReader.ListReader<Error.Location>() {
          @Override public Error.Location read(ResponseJsonStreamReader reader) throws IOException {
            return reader.nextObject(false, new ResponseJsonStreamReader.ObjectReader<Error.Location>() {
              @Override public Error.Location read(ResponseJsonStreamReader reader) throws IOException {
                return readErrorLocation(reader);
              }
            });
          }
        });
      } else {
        reader.skipNext();
      }
    }
    return new Error(message, locations);
  }

  @SuppressWarnings("ConstantConditions")
  private Error.Location readErrorLocation(ResponseJsonStreamReader reader) throws IOException {
    long line = -1;
    long column = -1;
    while (reader.hasNext()) {
      String name = reader.nextName();
      if ("line".equals(name)) {
        line = reader.nextLong(false);
      } else if ("column".equals(name)) {
        column = reader.nextLong(false);
      } else {
        reader.skipNext();
      }
    }
    return new Error.Location(line, column);
  }
}
