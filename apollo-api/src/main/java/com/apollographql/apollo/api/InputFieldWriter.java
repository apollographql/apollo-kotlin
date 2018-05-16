package com.apollographql.apollo.api;

import java.io.IOException;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public interface InputFieldWriter {
  void writeString(@NotNull String fieldName, String value) throws IOException;

  void writeInt(@NotNull String fieldName, Integer value) throws IOException;

  void writeLong(@NotNull String fieldName, Long value) throws IOException;

  void writeDouble(@NotNull String fieldName, Double value) throws IOException;

  void writeNumber(@NotNull String fieldName, Number value) throws IOException;

  void writeBoolean(@NotNull String fieldName, Boolean value) throws IOException;

  void writeCustom(@NotNull String fieldName, ScalarType scalarType, Object value) throws IOException;

  void writeObject(@NotNull String fieldName, InputFieldMarshaller marshaller) throws IOException;

  void writeList(@NotNull String fieldName, ListWriter listWriter) throws IOException;

  void writeMap(@NotNull String fieldName, Map<String, Object> value) throws IOException;

  interface ListWriter {
    void write(@NotNull ListItemWriter listItemWriter) throws IOException;
  }

  interface ListItemWriter {
    void writeString(String value) throws IOException;

    void writeInt(Integer value) throws IOException;

    void writeLong(Long value) throws IOException;

    void writeDouble(Double value) throws IOException;

    void writeNumber(Number value) throws IOException;

    void writeBoolean(Boolean value) throws IOException;

    void writeCustom(ScalarType scalarType, Object value) throws IOException;

    void writeObject(InputFieldMarshaller marshaller) throws IOException;

    void writeList(ListWriter listWriter) throws IOException;

    void writeMap(Map<String, Object> value) throws IOException;
  }
}
