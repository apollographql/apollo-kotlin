package com.apollographql.apollo.internal.json;

import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.Utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SortedInputFieldMapWriter implements InputFieldWriter {
  final Comparator<String> fieldNameComparator;
  final Map<String, Object> buffer;

  public SortedInputFieldMapWriter(@NotNull Comparator<String> fieldNameComparator) {
    this.fieldNameComparator = Utils.checkNotNull(fieldNameComparator, "fieldNameComparator == null");
    this.buffer = new TreeMap<>(fieldNameComparator);
  }

  public Map<String, Object> map() {
    return Collections.unmodifiableMap(buffer);
  }

  @Override public void writeString(@NotNull String fieldName, String value) throws IOException {
    buffer.put(fieldName, value);
  }

  @Override public void writeInt(@NotNull String fieldName, Integer value) throws IOException {
    buffer.put(fieldName, value);
  }

  @Override public void writeLong(@NotNull String fieldName, Long value) throws IOException {
    buffer.put(fieldName, value);
  }

  @Override public void writeDouble(@NotNull String fieldName, Double value) throws IOException {
    buffer.put(fieldName, value);
  }

  @Override public void writeNumber(@NotNull String fieldName, Number value) throws IOException {
    buffer.put(fieldName, value);
  }

  @Override public void writeBoolean(@NotNull String fieldName, Boolean value) throws IOException {
    buffer.put(fieldName, value);
  }

  @Override
  public void writeCustom(@NotNull String fieldName, ScalarType scalarType, Object value) throws IOException {
    buffer.put(fieldName, value);
  }

  @Override public void writeObject(@NotNull String fieldName, InputFieldMarshaller marshaller) throws IOException {
    if (marshaller == null) {
      buffer.put(fieldName, null);
    } else {
      SortedInputFieldMapWriter nestedWriter = new SortedInputFieldMapWriter(fieldNameComparator);
      marshaller.marshal(nestedWriter);
      buffer.put(fieldName, nestedWriter.buffer);
    }
  }

  @Override
  public void writeList(@NotNull String fieldName, ListWriter listWriter) throws IOException {
    if (listWriter == null) {
      buffer.put(fieldName, null);
    } else {
      ListItemWriter listItemWriter = new ListItemWriter(fieldNameComparator);
      listWriter.write(listItemWriter);
      buffer.put(fieldName, listItemWriter.list);
    }
  }

  @Override public void writeMap(@NotNull String fieldName, Map<String, Object> value) throws IOException {
    buffer.put(fieldName, value);
  }

  @SuppressWarnings("unchecked")
  private static class ListItemWriter implements InputFieldWriter.ListItemWriter {
    final Comparator<String> fieldNameComparator;
    final List list = new ArrayList();

    ListItemWriter(Comparator<String> fieldNameComparator) {
      this.fieldNameComparator = fieldNameComparator;
    }

    @Override public void writeString(String value) throws IOException {
      if (value != null) {
        list.add(value);
      }
    }

    @Override public void writeInt(Integer value) throws IOException {
      if (value != null) {
        list.add(value);
      }
    }

    @Override public void writeLong(Long value) throws IOException {
      if (value != null) {
        list.add(value);
      }
    }

    @Override public void writeDouble(Double value) throws IOException {
      if (value != null) {
        list.add(value);
      }
    }

    @Override public void writeNumber(Number value) throws IOException {
      if (value != null) {
        list.add(value);
      }
    }

    @Override public void writeBoolean(Boolean value) throws IOException {
      if (value != null) {
        list.add(value);
      }
    }

    @Override public void writeCustom(ScalarType scalarType, Object value) throws IOException {
      if (value != null) {
        list.add(value);
      }
    }

    @Override public void writeObject(InputFieldMarshaller marshaller) throws IOException {
      if (marshaller != null) {
        SortedInputFieldMapWriter nestedWriter = new SortedInputFieldMapWriter(fieldNameComparator);
        marshaller.marshal(nestedWriter);
        list.add(nestedWriter.buffer);
      }
    }

    @Override public void writeList(ListWriter listWriter) throws IOException {
      if (listWriter != null) {
        ListItemWriter nestedListItemWriter = new ListItemWriter(fieldNameComparator);
        listWriter.write(nestedListItemWriter);
        list.add(nestedListItemWriter.list);
      }
    }

    @Override public void writeMap(Map<String, Object> value) throws IOException {
      if (value != null) {
        list.add(value);
      }
    }
  }
}
