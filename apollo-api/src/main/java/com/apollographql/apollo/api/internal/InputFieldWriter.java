package com.apollographql.apollo.api.internal;

import com.apollographql.apollo.api.ScalarType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

public interface InputFieldWriter {
  void writeString(@NotNull String fieldName, @Nullable String value) throws IOException;

  void writeInt(@NotNull String fieldName, @Nullable Integer value) throws IOException;

  void writeLong(@NotNull String fieldName, @Nullable Long value) throws IOException;

  void writeDouble(@NotNull String fieldName, @Nullable Double value) throws IOException;

  void writeNumber(@NotNull String fieldName, @Nullable Number value) throws IOException;

  void writeBoolean(@NotNull String fieldName, @Nullable Boolean value) throws IOException;

  void writeCustom(@NotNull String fieldName, @NotNull ScalarType scalarType, @Nullable Object value)
      throws IOException;

  void writeObject(@NotNull String fieldName, @Nullable InputFieldMarshaller marshaller) throws IOException;

  void writeList(@NotNull String fieldName, @Nullable ListWriter listWriter) throws IOException;

  void writeMap(@NotNull String fieldName, @Nullable Map<String, Object> value) throws IOException;

  interface ListWriter {
    void write(@NotNull ListItemWriter listItemWriter) throws IOException;
  }

  interface ListItemWriter {
    void writeString(@Nullable String value) throws IOException;

    void writeInt(@Nullable Integer value) throws IOException;

    void writeLong(@Nullable Long value) throws IOException;

    void writeDouble(@Nullable Double value) throws IOException;

    void writeNumber(@Nullable Number value) throws IOException;

    void writeBoolean(@Nullable Boolean value) throws IOException;

    void writeCustom(@NotNull ScalarType scalarType, @Nullable Object value) throws IOException;

    void writeObject(@Nullable InputFieldMarshaller marshaller) throws IOException;

    void writeList(@Nullable ListWriter listWriter) throws IOException;

    void writeMap(@Nullable Map<String, Object> value) throws IOException;
  }
}
