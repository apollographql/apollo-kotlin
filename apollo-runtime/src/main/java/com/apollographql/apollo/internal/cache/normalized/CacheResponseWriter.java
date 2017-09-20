package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class CacheResponseWriter implements ResponseWriter {
  private final Operation.Variables operationVariables;
  private final ScalarTypeAdapters scalarTypeAdapters;
  final Map<String, FieldDescriptor> fieldDescriptors = new LinkedHashMap<>();

  CacheResponseWriter(Operation.Variables operationVariables, ScalarTypeAdapters scalarTypeAdapters) {
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
      fieldDescriptors.put(field.responseName(), new FieldDescriptor(field, null));
      return;
    }

    CacheResponseWriter nestedResponseWriter = new CacheResponseWriter(operationVariables, scalarTypeAdapters);
    marshaller.marshal(nestedResponseWriter);

    fieldDescriptors.put(field.responseName(), new FieldDescriptor(field, nestedResponseWriter.fieldDescriptors));
  }

  @Override
  public void writeList(@Nonnull ResponseField field, @Nullable List values, @Nonnull ListWriter listWriter) {
    checkFieldValue(field, values);

    if (values == null) {
      fieldDescriptors.put(field.responseName(), new FieldDescriptor(field, null));
      return;
    }

    List items = writeListItemValues(values, listWriter);
    fieldDescriptors.put(field.responseName(), new FieldDescriptor(field, items));
  }

  public Collection<Record> normalize(ResponseNormalizer<Map<String, Object>> responseNormalizer) {
    normalize(operationVariables, responseNormalizer, fieldDescriptors);
    return responseNormalizer.records();
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
    fieldDescriptors.put(field.responseName(), new FieldDescriptor(field, value));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> rawFieldValues(Map<String, FieldDescriptor> fieldDescriptors) {
    Map<String, Object> fieldValues = new LinkedHashMap<>();
    for (Map.Entry<String, FieldDescriptor> entry : fieldDescriptors.entrySet()) {
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

  @SuppressWarnings("unchecked") private void normalize(Operation.Variables operationVariables,
      ResponseNormalizer<Map<String, Object>> responseNormalizer, Map<String, FieldDescriptor> fieldDescriptors) {
    Map<String, Object> rawFieldValues = rawFieldValues(fieldDescriptors);
    for (String fieldResponseName : fieldDescriptors.keySet()) {
      FieldDescriptor fieldDescriptor = fieldDescriptors.get(fieldResponseName);
      Object rawFieldValue = rawFieldValues.get(fieldResponseName);
      responseNormalizer.willResolve(fieldDescriptor.field, operationVariables);

      switch (fieldDescriptor.field.type()) {
        case OBJECT: {
          normalizeObjectField(fieldDescriptor, (Map<String, Object>) rawFieldValue, responseNormalizer);
          break;
        }

        case LIST: {
          normalizeList(fieldDescriptor.field, (List) fieldDescriptor.value, (List) rawFieldValue, responseNormalizer);
          break;
        }

        default: {
          if (rawFieldValue == null) {
            responseNormalizer.didResolveNull();
          } else {
            responseNormalizer.didResolveScalar(rawFieldValue);
          }
          break;
        }
      }

      responseNormalizer.didResolve(fieldDescriptor.field, operationVariables);
    }
  }

  @SuppressWarnings("unchecked") private void normalizeObjectField(FieldDescriptor fieldDescriptor,
      Map<String, Object> rawFieldValues, ResponseNormalizer<Map<String, Object>> responseNormalizer) {
    responseNormalizer.willResolveObject(fieldDescriptor.field, Optional.fromNullable(rawFieldValues));
    if (fieldDescriptor.value == null) {
      responseNormalizer.didResolveNull();
    } else {
      normalize(operationVariables, responseNormalizer, (Map<String, FieldDescriptor>) fieldDescriptor.value);
    }
    responseNormalizer.didResolveObject(fieldDescriptor.field, Optional.fromNullable(rawFieldValues));
  }

  @SuppressWarnings("unchecked") private void normalizeList(ResponseField listResponseField, List fieldValues,
      List rawFieldValues, ResponseNormalizer<Map<String, Object>> responseNormalizer) {
    if (fieldValues == null) {
      responseNormalizer.didResolveNull();
      return;
    }

    for (int i = 0; i < fieldValues.size(); i++) {
      responseNormalizer.willResolveElement(i);

      Object fieldValue = fieldValues.get(i);
      if (fieldValue instanceof Map) {
        responseNormalizer.willResolveObject(listResponseField,
            Optional.fromNullable((Map<String, Object>) rawFieldValues.get(i)));
        normalize(operationVariables, responseNormalizer, (Map<String, FieldDescriptor>) fieldValue);
        responseNormalizer.didResolveObject(listResponseField,
            Optional.fromNullable((Map<String, Object>) rawFieldValues.get(i)));
      } else if (fieldValue instanceof List) {
        normalizeList(listResponseField, (List) fieldValue, (List) rawFieldValues.get(i), responseNormalizer);
      } else {
        responseNormalizer.didResolveScalar(rawFieldValues.get(i));
      }
      responseNormalizer.didResolveElement(i);
    }
    responseNormalizer.didResolveList(rawFieldValues);
  }

  private static void checkFieldValue(ResponseField field, Object value) {
    if (!field.optional() && value == null) {
      throw new NullPointerException(String.format("Mandatory response field `%s` resolved with null value",
          field.responseName()));
    }
  }

  @SuppressWarnings("unchecked") private static final class ListItemWriter implements ResponseWriter.ListItemWriter {
    final Operation.Variables operationVariables;
    final ScalarTypeAdapters scalarTypeAdapters;
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
      CacheResponseWriter nestedResponseWriter = new CacheResponseWriter(operationVariables, scalarTypeAdapters);
      marshaller.marshal(nestedResponseWriter);
      value = nestedResponseWriter.fieldDescriptors;
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
