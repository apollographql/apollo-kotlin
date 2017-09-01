package com.apollographql.apollo.api;

import java.util.List;

/*
 * ResponseReader is an abstraction for reading GraphQL fields.
 */
public interface ResponseReader {

  String readString(ResponseField field);

  Integer readInt(ResponseField field);

  Long readLong(ResponseField field);

  Double readDouble(ResponseField field);

  Boolean readBoolean(ResponseField field);

  <T> T readObject(ResponseField field, ObjectReader<T> objectReader);

  <T> List<T> readList(ResponseField field, ListReader<T> listReader);

  <T> T readCustomType(ResponseField.CustomTypeField field);

  <T> T readConditional(ResponseField field, ConditionalTypeReader<T> conditionalTypeReader);

  interface ObjectReader<T> {
    T read(ResponseReader reader);
  }

  interface ListReader<T> {
    T read(ListItemReader reader);
  }

  interface ConditionalTypeReader<T> {
    T read(String conditionalType, ResponseReader reader);
  }

  interface ListItemReader {

    String readString();

    Integer readInt();

    Long readLong();

    Double readDouble();

    Boolean readBoolean();

    <T> T readCustomType(ScalarType scalarType);

    <T> T readObject(ObjectReader<T> objectReader);

    <T> List<T> readList(ListReader<T> listReader);
  }
}