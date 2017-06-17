package com.apollographql.apollo.api;

import java.io.IOException;
import java.util.List;

/*
 * ResponseReader is an abstraction for reading GraphQL fields.
 */
public interface ResponseReader {

  String readString(Field field) throws IOException;

  Integer readInt(Field field) throws IOException;

  Long readLong(Field field) throws IOException;

  Double readDouble(Field field) throws IOException;

  Boolean readBoolean(Field field) throws IOException;

  <T> T readObject(Field field, ResponseReader.ObjectReader<T> objectReader) throws IOException;

  <T> List<T> readList(Field field, ResponseReader.ListReader listReader) throws IOException;

  <T> T readCustomType(Field.CustomTypeField field) throws IOException;

  <T> T readConditional(Field.ConditionalTypeField field, ConditionalTypeReader<T> conditionalTypeReader)
      throws IOException;

  interface ObjectReader<T> {
    T read(ResponseReader reader) throws IOException;
  }

  interface ListReader<T> {
    T read(ListItemReader reader) throws IOException;
  }

  interface ConditionalTypeReader<T> {
    T read(String conditionalType, ResponseReader reader) throws IOException;
  }

  interface ListItemReader {

    String readString() throws IOException;

    Integer readInt() throws IOException;

    Long readLong() throws IOException;

    Double readDouble() throws IOException;

    Boolean readBoolean() throws IOException;

    <T> T readCustomType(ScalarType scalarType) throws IOException;

    <T> T readObject(ResponseReader.ObjectReader<T> objectReader) throws IOException;
  }
}