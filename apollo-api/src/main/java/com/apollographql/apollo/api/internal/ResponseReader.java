package com.apollographql.apollo.api.internal;

import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ScalarType;

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

  <T> T readFragment(ResponseField field, ObjectReader<T> objectReader);

  <T> List<T> readList(ResponseField field, ListReader<T> listReader);

  @SuppressWarnings("TypeParameterUnusedInFormals")
  <T> T readCustomType(ResponseField.CustomTypeField field);

  interface ObjectReader<T> {
    T read(ResponseReader reader);
  }

  interface ListReader<T> {
    T read(ListItemReader reader);
  }

  interface ListItemReader {

    String readString();

    Integer readInt();

    Long readLong();

    Double readDouble();

    Boolean readBoolean();

    @SuppressWarnings("TypeParameterUnusedInFormals")
    <T> T readCustomType(ScalarType scalarType);

    <T> T readObject(ObjectReader<T> objectReader);

    <T> List<T> readList(ListReader<T> listReader);
  }
}
