package com.apollostack.api.graphql;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** TODO java doc **/
public interface ResponseReader {
  interface NestedReader<T> {
    T read(ResponseReader streamReader) throws IOException;
  }

  String readString(String responseName, String fieldName, Map<String, Object> arguments) throws IOException;

  String readOptionalString(String responseName, String fieldName, Map<String, Object> arguments) throws IOException;

  int readInt(String responseName, String fieldName, Map<String, Object> arguments) throws IOException;

  Integer readOptionalInt(String responseName, String fieldName, Map<String, Object> arguments) throws IOException;

  long readLong(String responseName, String fieldName, Map<String, Object> arguments) throws IOException;

  Long readOptionalLong(String responseName, String fieldName, Map<String, Object> arguments) throws IOException;

  double readDouble(String responseName, String fieldName, Map<String, Object> arguments) throws IOException;

  Double readOptionalDouble(String responseName, String fieldName, Map<String, Object> arguments) throws IOException;

  boolean readBoolean(String responseName, String fieldName, Map<String, Object> arguments) throws IOException;

  Boolean readOptionalBoolean(String responseName, String fieldName, Map<String, Object> arguments) throws IOException;

  <T> T readObject(String responseName, String fieldName, Map<String, Object> arguments, NestedReader<T> reader) throws IOException;

  <T> T readOptionalObject(String responseName, String fieldName, Map<String, Object> arguments, NestedReader<T> reader) throws IOException;

  <T> List<T> readList(String responseName, String fieldName, Map<String, Object> arguments, NestedReader<T> reader) throws IOException;

  <T> List<T> readOptionalList(String responseName, String fieldName, Map<String, Object> arguments, NestedReader<T> reader) throws IOException;
}
