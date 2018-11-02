package com.apollographql.apollo.api;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ResponseWriter {
  void writeString(@NotNull ResponseField field, @Nullable String value);

  void writeInt(@NotNull ResponseField field, @Nullable Integer value);

  void writeLong(@NotNull ResponseField field, @Nullable Long value);

  void writeDouble(@NotNull ResponseField field, @Nullable Double value);

  void writeBoolean(@NotNull ResponseField field, @Nullable Boolean value);

  void writeCustom(@NotNull ResponseField.CustomTypeField field, @Nullable Object value);

  void writeObject(@NotNull ResponseField field, @Nullable ResponseFieldMarshaller marshaller);

  void writeList(@NotNull ResponseField field, @Nullable List values, @NotNull ListWriter listWriter);

  interface ListWriter {
    void write(@Nullable List items, @NotNull ListItemWriter listItemWriter);
  }

  interface ListItemWriter {
    void writeString(@Nullable String value);

    void writeInt(@Nullable Integer value);

    void writeLong(@Nullable Long value);

    void writeDouble(@Nullable Double value);

    void writeBoolean(@Nullable Boolean value);

    void writeCustom(@NotNull ScalarType scalarType, @Nullable Object value);

    void writeObject(@Nullable ResponseFieldMarshaller marshaller);

    void writeList(@Nullable List items, @NotNull ListWriter listWriter);
  }
}
