package com.apollographql.apollo.api;

import java.io.IOException;

import javax.annotation.Nonnull;

public interface InputFieldWriter {
  void writeString(@Nonnull String fieldName, String value) throws IOException;

  void writeInt(@Nonnull String fieldName, Integer value) throws IOException;

  void writeLong(@Nonnull String fieldName, Long value) throws IOException;

  void writeDouble(@Nonnull String fieldName, Double value) throws IOException;

  void writeBoolean(@Nonnull String fieldName, Boolean value) throws IOException;

  void writeCustom(@Nonnull String fieldName, ScalarType scalarType, Object value) throws IOException;

  void writeObject(@Nonnull String fieldName, InputFieldMarshaller marshaller) throws IOException;

  void writeList(@Nonnull String fieldName, ListWriter listWriter) throws IOException;

  interface ListWriter {
    void write(@Nonnull ListItemWriter listItemWriter) throws IOException;
  }

  interface ListItemWriter {
    void writeString(String value) throws IOException;

    void writeInt(Integer value) throws IOException;

    void writeLong(Long value) throws IOException;

    void writeDouble(Double value) throws IOException;

    void writeBoolean(Boolean value) throws IOException;

    void writeCustom(ScalarType scalarType, Object value) throws IOException;

    void writeObject(InputFieldMarshaller marshaller) throws IOException;

    void writeList(ListWriter listWriter) throws IOException;
  }
}
