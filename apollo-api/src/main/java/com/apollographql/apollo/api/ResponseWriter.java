package com.apollographql.apollo.api;

import java.io.IOException;

public interface ResponseWriter {
  void writeString(ResponseField field, String value) throws IOException;

  void writeInt(ResponseField field, Integer value) throws IOException;

  void writeLong(ResponseField field, Long value) throws IOException;

  void writeDouble(ResponseField field, Double value) throws IOException;

  void writeBoolean(ResponseField field, Boolean value) throws IOException;

  void writeCustom(ResponseField.CustomTypeField field, Object value) throws IOException;

  void writeObject(ResponseField field, ResponseFieldMarshaller marshaller) throws IOException;

  void writeList(ResponseField field, ListWriter listWriter) throws IOException;

  interface ListWriter {
    void write(ListItemWriter listItemWriter) throws IOException;
  }

  interface ListItemWriter {
    void writeString(String value) throws IOException;

    void writeInt(Integer value) throws IOException;

    void writeLong(Long value) throws IOException;

    void writeDouble(Double value) throws IOException;

    void writeBoolean(Boolean value) throws IOException;

    void writeCustom(ScalarType scalarType, Object value) throws IOException;

    void writeObject(ResponseFieldMarshaller marshaller) throws IOException;
  }
}
