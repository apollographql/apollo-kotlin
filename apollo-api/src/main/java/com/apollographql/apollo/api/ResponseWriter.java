package com.apollographql.apollo.api;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ResponseWriter {
  void writeString(@Nonnull ResponseField field, @Nullable String value);

  void writeInt(@Nonnull ResponseField field, @Nullable Integer value);

  void writeLong(@Nonnull ResponseField field, @Nullable Long value);

  void writeDouble(@Nonnull ResponseField field, @Nullable Double value);

  void writeBoolean(@Nonnull ResponseField field, @Nullable Boolean value);

  void writeCustom(@Nonnull ResponseField.CustomTypeField field, @Nullable Object value);

  void writeObject(@Nonnull ResponseField field, @Nullable ResponseFieldMarshaller marshaller);

  void writeList(@Nonnull ResponseField field, @Nullable List values, @Nonnull ListWriter listWriter);

  interface ListWriter {
    void write(@Nullable Object value, @Nonnull ListItemWriter listItemWriter);
  }

  interface ListItemWriter {
    void writeString(@Nullable Object value);

    void writeInt(@Nullable Object value);

    void writeLong(@Nullable Object value);

    void writeDouble(@Nullable Object value);

    void writeBoolean(@Nullable Object value);

    void writeCustom(@Nonnull ScalarType scalarType, @Nullable Object value);

    void writeObject(@Nullable ResponseFieldMarshaller marshaller);
  }
}
