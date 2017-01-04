package com.apollostack.api.graphql;

import java.util.List;

/** TODO java doc **/
public interface ResponseReader {
  interface NestedReader<T> {
    T read(ResponseReader streamReader);
  }

  interface Converter<T> {
    T convert(Object from);
  }

  String readString(String responseName, String fieldName);

  String readOptionalString(String responseName, String fieldName);

  int readInt(String responseName, String fieldName);

  Integer readOptionalInt(String responseName, String fieldName);

  double readDouble(String responseName, String fieldName);

  Double readOptionalDouble(String responseName, String fieldName);

  boolean readBoolean(String responseName, String fieldName);

  Boolean readOptionalBoolean(String responseName, String fieldName);

  <T> T readObject(String responseName, String fieldName, NestedReader<T> reader);

  <T> T readOptionalObject(String responseName, String fieldName, NestedReader<T> reader);

  <T> List<T> readList(String responseName, String fieldName, NestedReader<T> reader);

  <T> List<T> readOptionalList(String responseName, String fieldName, NestedReader<T> reader);

  <T> List<T> readList(String responseName, String fieldName, Converter<T> converter);

  <T> List<T> readOptionalList(String responseName, String fieldName, Converter<T> converter);
}
