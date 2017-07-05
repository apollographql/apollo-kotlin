package com.apollographql.apollo.api;

public interface ResponseWriter {
  void writeString(ResponseField field, String value);

  void writeInt(ResponseField field, Integer value);

  void writeLong(ResponseField field, Long value);

  void writeDouble(ResponseField field, Double value);

  void writeBoolean(ResponseField field, Boolean value);

  void writeCustom(ResponseField.CustomTypeField field, Object value);

  void writeObject(ResponseField field, ResponseFieldMarshaller marshaller);

  void writeList(ResponseField field, ListWriter listWriter);

  interface ListWriter {
    void write(ListItemWriter listItemWriter);
  }

  interface ListItemWriter {
    void writeString(String value);

    void writeInt(Integer value);

    void writeLong(Long value);

    void writeDouble(Double value);

    void writeBoolean(Boolean value);

    void writeCustom(ScalarType scalarType, Object value);

    void writeObject(ResponseFieldMarshaller marshaller);
  }
}
