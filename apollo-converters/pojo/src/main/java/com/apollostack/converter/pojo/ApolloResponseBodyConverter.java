package com.apollostack.converter.pojo;

import com.apollostack.api.graphql.Error;
import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.Response;
import com.apollostack.api.graphql.ResponseStreamReader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Converter;

class ApolloResponseBodyConverter implements Converter<ResponseBody, Response<? extends Operation.Data>> {
  private final Type type;

  ApolloResponseBodyConverter(Type type) {
    this.type = type;
  }

  @Override public Response<? extends Operation.Data> convert(ResponseBody value) throws IOException {
    BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(value.source());
    jsonReader.beginObject();

    ResponseStreamReader responseStreamReader = new ResponseJsonStreamReader(jsonReader);
    Operation.Data data = null;
    List<Error> errors = null;
    while (responseStreamReader.hasNext()) {
      String name = responseStreamReader.nextName();
      if ("data".equals(name)) {
        data = readResponseData(responseStreamReader);
      } else if ("errors".equals(name)) {
        errors = readResponseErrors(responseStreamReader);
      } else {
        responseStreamReader.skipNext();
      }
    }
    jsonReader.endObject();

    return new Response<>(data, errors);
  }

  private Operation.Data readResponseData(ResponseStreamReader reader) throws IOException {
    return reader.nextObject(new ResponseStreamReader.NestedReader<Operation.Data>() {
      @Override public Operation.Data read(ResponseStreamReader reader) throws IOException {
        //noinspection TryWithIdenticalCatches
        try {
          return (Operation.Data) ((Class<?>) type).getConstructor(ResponseStreamReader.class).newInstance(reader);
        } catch (InstantiationException e) {
          throw new RuntimeException("Can't instantiate " + type, e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException("Can't instantiate " + type, e);
        } catch (InvocationTargetException e) {
          throw new RuntimeException("Can't instantiate " + type, e);
        } catch (NoSuchMethodException e) {
          throw new RuntimeException("Can't instantiate " + type, e);
        }
      }
    });
  }

  private List<Error> readResponseErrors(ResponseStreamReader reader) throws IOException {
    return reader.nextList(new ResponseStreamReader.NestedReader<Error>() {
      @Override public Error read(ResponseStreamReader reader) throws IOException {
        return readError(reader);
      }
    });
  }

  private Error readError(ResponseStreamReader reader) throws IOException {
    String message = null;
    List<Error.Location> locations = null;
    while (reader.hasNext()) {
      String name = reader.nextName();
      if ("message".equals(name)) {
        message = reader.nextString();
      } else if ("locations".equals(name)) {
        locations = reader.nextList(new ResponseStreamReader.NestedReader<Error.Location>() {
          @Override public Error.Location read(ResponseStreamReader reader) throws IOException {
            return readErrorLocation(reader);
          }
        });
      } else {
        reader.skipNext();
      }
    }
    return new Error(message, locations);
  }

  private Error.Location readErrorLocation(ResponseStreamReader reader) throws IOException {
    long line = -1;
    long column = -1;
    while (reader.hasNext()) {
      String name = reader.nextName();
      if ("line".equals(name)) {
        line = reader.nextLong();
      } else if ("column".equals(name)) {
        column = reader.nextLong();
      } else {
        reader.skipNext();
      }
    }
    return new Error.Location(line, column);
  }
}
