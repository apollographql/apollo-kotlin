package com.apollographql.android.impl;

import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.api.graphql.Error;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ScalarType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;

final class HttpResponseBodyConverter {
  private final Operation operation;
  private final ResponseFieldMapper responseFieldMapper;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;

  HttpResponseBodyConverter(Operation operation, ResponseFieldMapper responseFieldMapper,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
    this.operation = operation;
    this.responseFieldMapper = responseFieldMapper;
    this.customTypeAdapters = customTypeAdapters;
  }

  <T extends Operation.Data> Response<T> convert(ResponseBody responseBody,
      final ResponseReaderShadow<Map<String, Object>> responseReaderShadow) throws IOException {
    responseReaderShadow.willResolveRootQuery(operation);
    BufferedSourceJsonReader jsonReader = null;
    try {
      jsonReader = new BufferedSourceJsonReader(responseBody.source());
      jsonReader.beginObject();

      ResponseJsonStreamReader responseStreamReader = new ResponseJsonStreamReader(jsonReader);
      T data = null;
      List<Error> errors = null;
      while (responseStreamReader.hasNext()) {
        String name = responseStreamReader.nextName();
        if ("data".equals(name)) {
          //noinspection unchecked
          data = (T) responseStreamReader.nextObject(false, new ResponseJsonStreamReader.ObjectReader<Object>() {
            @Override public Object read(ResponseJsonStreamReader reader) throws IOException {
              Map<String, Object> buffer = reader.buffer();
              RealResponseReader<Map<String, Object>> realResponseReader = new RealResponseReader<>(operation, buffer,
                  new MapFieldValueResolver(), customTypeAdapters, responseReaderShadow);
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
      return new Response<>(operation, data, errors);
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
