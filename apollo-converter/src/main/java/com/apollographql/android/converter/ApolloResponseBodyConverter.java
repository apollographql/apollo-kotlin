package com.apollographql.android.converter;

import com.apollographql.android.api.graphql.Error;
import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.ScalarType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Converter;

final class ApolloResponseBodyConverter implements Converter<ResponseBody, Response<? extends Operation.Data>> {
  private final ResponseFieldMapper responseFieldMapper;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;

  ApolloResponseBodyConverter(ResponseFieldMapper responseFieldMapper,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
    this.responseFieldMapper = responseFieldMapper;
    this.customTypeAdapters = customTypeAdapters;
  }

  @Override public Response<? extends Operation.Data> convert(ResponseBody value) throws IOException {
    BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(value.source());
    jsonReader.beginObject();

    ResponseJsonStreamReader responseStreamReader = new ResponseJsonStreamReader(jsonReader, customTypeAdapters);
    Operation.Data data = null;
    List<Error> errors = null;
    while (responseStreamReader.hasNext()) {
      String name = responseStreamReader.nextName();
      if ("data".equals(name)) {
        data = (Operation.Data) responseStreamReader.nextObject(false, new Field.ObjectReader<Object>() {
          @Override public Object read(ResponseReader reader) throws IOException {
            return responseFieldMapper.map(reader);
          }
        });
      } else if ("errors".equals(name)) {
        errors = readResponseErrors(responseStreamReader);
      } else {
        responseStreamReader.skipNext();
      }
    }
    jsonReader.endObject();

    return new Response<>(data, errors);
  }

  private List<Error> readResponseErrors(ResponseJsonStreamReader reader) throws IOException {
    return reader.nextList(true, new Field.ObjectReader<Error>() {
      @Override public Error read(ResponseReader reader) throws IOException {
        return readError((ResponseJsonStreamReader) reader);
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
        locations = reader.nextList(true, new Field.ObjectReader<Error.Location>() {
          @Override public Error.Location read(ResponseReader reader) throws IOException {
            return readErrorLocation((ResponseJsonStreamReader) reader);
          }
        });
      } else {
        reader.skipNext();
      }
    }
    return new Error(message, locations);
  }

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
