package com.apollographql.apollo.internal.response;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RealResponseWriter implements ResponseWriter {
  private final Operation.Variables operationVariables;
  private final ScalarTypeAdapters scalarTypeAdapters;
  final Map<String, FieldDescriptor> buffer = new LinkedHashMap<>();

  public RealResponseWriter(Operation.Variables operationVariables, ScalarTypeAdapters scalarTypeAdapters) {
    this.operationVariables = operationVariables;
    this.scalarTypeAdapters = scalarTypeAdapters;
  }

  @Override public void writeString(@Nonnull ResponseField field, @Nullable String value) {
    writeScalarFieldValue(field, value);
  }

  @Override public void writeInt(@Nonnull ResponseField field, @Nullable Integer value) {
    writeScalarFieldValue(field, value != null ? BigDecimal.valueOf(value) : null);
  }

  @Override public void writeLong(@Nonnull ResponseField field, @Nullable Long value) {
    writeScalarFieldValue(field, value != null ? BigDecimal.valueOf(value) : null);
  }

  @Override public void writeDouble(@Nonnull ResponseField field, @Nullable Double value) {
    writeScalarFieldValue(field, value != null ? BigDecimal.valueOf(value) : null);
  }

  @Override public void writeBoolean(@Nonnull ResponseField field, @Nullable Boolean value) {
    writeScalarFieldValue(field, value);
  }

  @Override public void writeCustom(@Nonnull ResponseField.CustomTypeField field, @Nullable Object value) {
    CustomTypeAdapter typeAdapter = scalarTypeAdapters.adapterFor(field.scalarType());
    writeScalarFieldValue(field, value != null ? typeAdapter.encode(value) : null);
  }

  @Override public void writeObject(@Nonnull ResponseField field, @Nullable ResponseFieldMarshaller marshaller) {
    checkFieldValue(field, marshaller);
    if (marshaller == null) {
      buffer.put(field.responseName(), new FieldDescriptor(field, null));
      return;
    }


    RealResponseWriter nestedResponseWriter = new RealResponseWriter(operationVariables, scalarTypeAdapters);
    marshaller.marshal(nestedResponseWriter);

    buffer.put(field.responseName(), new FieldDescriptor(field, nestedResponseWriter.buffer));
  }

  @Override
  public void writeList(@Nonnull ResponseField field, @Nullable List values, @Nonnull ListWriter listWriter) {
    checkFieldValue(field, values);

    if (values == null) {
      buffer.put(field.responseName(), new FieldDescriptor(field, null));
      return;
    }

    List items = writeListItemValues(values, listWriter);
    buffer.put(field.responseName(), new FieldDescriptor(field, items));
  }

  public void resolveFields(ResolveDelegate<Map<String, Object>> delegate) {
    resolveFields(operationVariables, delegate, buffer);
  }

  @SuppressWarnings("unchecked") private List writeListItemValues(List values, ListWriter listWriter) {
    ListItemWriter listItemWriter = new ListItemWriter(operationVariables, scalarTypeAdapters);
    List items = new ArrayList();
    for (Object value : values) {
      if (value instanceof List) {
        List nestedItems = writeListItemValues((List) value, listWriter);
        items.add(nestedItems);
      } else {
        listWriter.write(value, listItemWriter);
        items.add(listItemWriter.value);
      }
    }
    return items;
  }

  private void writeScalarFieldValue(ResponseField field, Object value) {
    checkFieldValue(field, value);
    buffer.put(field.responseName(), new FieldDescriptor(field, value));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> rawFieldValues(Map<String, FieldDescriptor> buffer) {
    Map<String, Object> fieldValues = new LinkedHashMap<>();
    for (Map.Entry<String, FieldDescriptor> entry : buffer.entrySet()) {
      String fieldResponseName = entry.getKey();
      Object fieldValue = entry.getValue().value;
      if (fieldValue == null) {
        fieldValues.put(fieldResponseName, null);
      } else if (fieldValue instanceof Map) {
        Map<String, Object> nestedMap = rawFieldValues((Map<String, FieldDescriptor>) fieldValue);
        fieldValues.put(fieldResponseName, nestedMap);
      } else if (fieldValue instanceof List) {
        fieldValues.put(fieldResponseName, rawListFieldValues((List) fieldValue));
      } else {
        fieldValues.put(fieldResponseName, fieldValue);
      }
    }
    return fieldValues;
  }

  @SuppressWarnings("unchecked") private List rawListFieldValues(List values) {
    List listValues = new ArrayList();
    for (Object value : values) {
      if (value instanceof Map) {
        listValues.add(rawFieldValues((Map<String, FieldDescriptor>) value));
      } else if (value instanceof List) {
        listValues.add(rawListFieldValues((List) value));
      } else {
        listValues.add(value);
      }
    }
    return listValues;
  }

  @SuppressWarnings("unchecked") private void resolveFields(Operation.Variables operationVariables,
      ResolveDelegate<Map<String, Object>> delegate, Map<String, FieldDescriptor> buffer) {
    Map<String, Object> rawFieldValues = rawFieldValues(buffer);
    for (String fieldResponseName : buffer.keySet()) {
      FieldDescriptor fieldDescriptor = buffer.get(fieldResponseName);
      Object rawFieldValue = rawFieldValues.get(fieldResponseName);
      delegate.willResolve(fieldDescriptor.field, operationVariables);

      switch (fieldDescriptor.field.type()) {
        case OBJECT: {
          resolveObjectFields(fieldDescriptor, (Map<String, Object>) rawFieldValue, delegate);
          break;
        }

        case LIST: {
          resolveListField(fieldDescriptor.field, (List) fieldDescriptor.value, (List) rawFieldValue, delegate);
          break;
        }

        default: {
          if (rawFieldValue == null) {
            delegate.didResolveNull();
          } else {
            delegate.didResolveScalar(rawFieldValue);
          }
          break;
        }
      }

      delegate.didResolve(fieldDescriptor.field, operationVariables);
    }
  }

  @SuppressWarnings("unchecked") private void resolveObjectFields(FieldDescriptor fieldDescriptor,
      Map<String, Object> rawFieldValues, ResolveDelegate<Map<String, Object>> delegate) {
    delegate.willResolveObject(fieldDescriptor.field, Optional.fromNullable(rawFieldValues));
    if (fieldDescriptor.value == null) {
      delegate.didResolveNull();
    } else {
      resolveFields(operationVariables, delegate, (Map<String, FieldDescriptor>) fieldDescriptor.value);
    }
    delegate.didResolveObject(fieldDescriptor.field, Optional.fromNullable(rawFieldValues));
  }

  @SuppressWarnings("unchecked") private void resolveListField(ResponseField listResponseField, List fieldValues,
      List rawFieldValues, ResolveDelegate<Map<String, Object>> delegate) {
    if (fieldValues == null) {
      delegate.didResolveNull();
      return;
    }

    for (int i = 0; i < fieldValues.size(); i++) {
      delegate.willResolveElement(i);

      Object fieldValue = fieldValues.get(i);
      if (fieldValue instanceof Map) {
        delegate.willResolveObject(listResponseField,
            Optional.fromNullable((Map<String, Object>) rawFieldValues.get(i)));
        resolveFields(operationVariables, delegate, (Map<String, FieldDescriptor>) fieldValue);
        delegate.didResolveObject(listResponseField,
            Optional.fromNullable((Map<String, Object>) rawFieldValues.get(i)));
      } else if (fieldValue instanceof List) {
        resolveListField(listResponseField, (List) fieldValue, (List) rawFieldValues.get(i), delegate);
      } else {
        delegate.didResolveScalar(rawFieldValues.get(i));
      }
      delegate.didResolveElement(i);
    }
    delegate.didResolveList(rawFieldValues);
  }

  private static void checkFieldValue(ResponseField field, Object value) {
    if (!field.optional() && value == null) {
      throw new NullPointerException(String.format("Mandatory response field `%s` resolved with null value",
          field.responseName()));
    }
  }

  @SuppressWarnings("unchecked") private static final class ListItemWriter implements ResponseWriter.ListItemWriter {
    final Operation.Variables operationVariables;
    final com.apollographql.apollo.response.ScalarTypeAdapters scalarTypeAdapters;
    Object value;

    ListItemWriter(Operation.Variables operationVariables, ScalarTypeAdapters scalarTypeAdapters) {
      this.operationVariables = operationVariables;
      this.scalarTypeAdapters = scalarTypeAdapters;
    }

    @Override public void writeString(@Nullable Object value) {
      this.value = value;
    }

    @Override public void writeInt(@Nullable Object value) {
      this.value = value != null ? BigDecimal.valueOf((Integer) value) : null;
    }

    @Override public void writeLong(@Nullable Object value) {
      this.value = value != null ? BigDecimal.valueOf((Long) value) : null;
    }

    @Override public void writeDouble(@Nullable Object value) {
      this.value = value != null ? BigDecimal.valueOf((Double) value) : null;
    }

    @Override public void writeBoolean(@Nullable Object value) {
      this.value = value;
    }

    @Override public void writeCustom(@Nonnull ScalarType scalarType, @Nullable Object value) {
      CustomTypeAdapter typeAdapter = scalarTypeAdapters.adapterFor(scalarType);
      this.value = value != null ? typeAdapter.encode(value) : null;
    }

    @Override public void writeObject(ResponseFieldMarshaller marshaller) {
      RealResponseWriter nestedResponseWriter = new RealResponseWriter(operationVariables, scalarTypeAdapters);
      marshaller.marshal(nestedResponseWriter);
      value = nestedResponseWriter.buffer;
    }
  }

  private static class FieldDescriptor {
    final ResponseField field;
    final Object value;

    FieldDescriptor(ResponseField field, Object value) {
      this.field = field;
      this.value = value;
    }
  }
}
