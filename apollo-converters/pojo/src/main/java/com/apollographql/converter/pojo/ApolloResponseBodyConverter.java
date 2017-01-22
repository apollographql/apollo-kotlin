package com.apollographql.converter.pojo;

import com.apollographql.api.graphql.Error;
import com.apollographql.api.graphql.Field;
import com.apollographql.api.graphql.Operation;
import com.apollographql.api.graphql.Response;
import com.apollographql.api.graphql.ResponseReader;

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

    ResponseJsonStreamReader responseStreamReader = new ResponseJsonStreamReader(jsonReader);
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

  private Operation.Data readResponseData(ResponseJsonStreamReader reader) throws IOException {
    return reader.nextObject(true, new Field.ObjectReader<Operation.Data>() {
      @Override public Operation.Data read(ResponseReader reader) throws IOException {
        //noinspection TryWithIdenticalCatches
        try {
          return (Operation.Data) ((Class<?>) type).getConstructor(ResponseReader.class).newInstance(reader);
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

  private List<Error> readResponseErrors(ResponseJsonStreamReader reader) throws IOException {
    return reader.nextList(true, new Field.ObjectReader<Error>() {
      @Override public Error read(ResponseReader reader) throws IOException {
        return ((ResponseJsonStreamReader) reader).nextObject(false, new Field.ObjectReader<Error>() {
          @Override public Error read(ResponseReader reader) throws IOException {
            return readError((ResponseJsonStreamReader) reader);
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
        locations = reader.nextList(true, new Field.ObjectReader<Error.Location>() {
          @Override public Error.Location read(ResponseReader reader) throws IOException {
            return ((ResponseJsonStreamReader) reader).nextObject(false, new Field.ObjectReader<Error.Location>() {
              @Override public Error.Location read(ResponseReader reader) throws IOException {
                return readErrorLocation((ResponseJsonStreamReader) reader);
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
