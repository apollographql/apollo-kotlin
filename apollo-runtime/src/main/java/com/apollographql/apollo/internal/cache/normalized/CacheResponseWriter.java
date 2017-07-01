package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.Record;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CacheResponseWriter implements ResponseWriter {
  private final Operation operation;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  final Map<String, FieldDescriptor> fieldDescriptors = new LinkedHashMap<>();
  final Map<String, Object> fieldValues = new LinkedHashMap<>();

  CacheResponseWriter(Operation operation, Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
    this.operation = operation;
    this.customTypeAdapters = customTypeAdapters;
  }

  @Override public void writeString(ResponseField field, String value) throws IOException {
    writeScalarFieldValue(field, value);
  }

  @Override public void writeInt(ResponseField field, Integer value) throws IOException {
    writeScalarFieldValue(field, value != null ? BigDecimal.valueOf(value) : null);
  }

  @Override public void writeLong(ResponseField field, Long value) throws IOException {
    writeScalarFieldValue(field, value != null ? BigDecimal.valueOf(value) : null);
  }

  @Override public void writeDouble(ResponseField field, Double value) throws IOException {
    writeScalarFieldValue(field, value != null ? BigDecimal.valueOf(value) : null);
  }

  @Override public void writeBoolean(ResponseField field, Boolean value) throws IOException {
    writeScalarFieldValue(field, value);
  }

  @Override public void writeCustom(ResponseField.CustomTypeField field, Object value) throws IOException {
    CustomTypeAdapter typeAdapter = customTypeAdapters.get(field.scalarType());
    if (typeAdapter == null) {
      writeScalarFieldValue(field, value);
    } else {
      writeScalarFieldValue(field, value != null ? typeAdapter.encode(value) : null);
    }
  }

  @Override public void writeObject(ResponseField field, ResponseFieldMarshaller marshaller) throws IOException {
    checkFieldValue(field, marshaller);
    if (marshaller == null) {
      fieldDescriptors.put(field.responseName(), new ObjectFieldDescriptor(field,
          Collections.<String, FieldDescriptor>emptyMap()));
      return;
    }

    CacheResponseWriter nestedResponseWriter = new CacheResponseWriter(operation, customTypeAdapters);
    marshaller.marshal(nestedResponseWriter);

    fieldDescriptors.put(field.responseName(), new ObjectFieldDescriptor(field, nestedResponseWriter.fieldDescriptors));
    fieldValues.put(field.responseName(), nestedResponseWriter.fieldValues);
  }

  @Override public void writeList(ResponseField field, ListWriter listWriter) throws IOException {
    checkFieldValue(field, listWriter);

    if (listWriter == null) {
      fieldDescriptors.put(field.responseName(), new ListFieldDescriptor(field,
          Collections.<Map<String, FieldDescriptor>>emptyList()));
      return;
    }

    ListItemWriter listItemWriter = new ListItemWriter(operation, customTypeAdapters);
    listWriter.write(listItemWriter);

    fieldDescriptors.put(field.responseName(), new ListFieldDescriptor(field, listItemWriter.fieldDescriptors));
    fieldValues.put(field.responseName(), listItemWriter.fieldValues);
  }

  public Collection<Record> normalize(ResponseNormalizer<Map<String, Object>> responseNormalizer) {
    responseNormalizer.willResolveRootQuery(operation);
    normalize(operation, responseNormalizer, fieldDescriptors, fieldValues);
    return responseNormalizer.records();
  }

  private void writeScalarFieldValue(ResponseField field, Object value) {
    checkFieldValue(field, value);
    fieldDescriptors.put(field.responseName(), new FieldDescriptor(field));
    if (value != null) {
      fieldValues.put(field.responseName(), value);
    }
  }

  @SuppressWarnings("unchecked") private void normalize(Operation operation,
      ResponseNormalizer<Map<String, Object>> responseNormalizer, Map<String, FieldDescriptor> fieldDescriptors,
      Map<String, Object> fieldValues) {
    for (String fieldResponseName : fieldDescriptors.keySet()) {
      FieldDescriptor fieldDescriptor = fieldDescriptors.get(fieldResponseName);
      Object fieldValue = fieldValues.get(fieldResponseName);
      responseNormalizer.willResolve(fieldDescriptor.field, operation.variables());

      switch (fieldDescriptor.field.type()) {
        case OBJECT: {
          ObjectFieldDescriptor objectFieldDescriptor = (ObjectFieldDescriptor) fieldDescriptor;
          Map<String, Object> objectFieldValues = (Map<String, Object>) fieldValue;
          normalizeObjectField(objectFieldDescriptor, objectFieldValues, responseNormalizer);
          break;
        }

        case OBJECT_LIST: {
          ListFieldDescriptor listFieldDescriptor = (ListFieldDescriptor) fieldDescriptor;
          List<Map<String, Object>> listFieldValues = (List<Map<String, Object>>) fieldValue;
          normalizeObjectListField(listFieldDescriptor, listFieldValues, responseNormalizer);
          break;
        }

        case CUSTOM_LIST:
        case SCALAR_LIST: {
          normalizeScalarList((List) fieldValue, responseNormalizer);
          break;
        }

        default: {
          if (fieldValue == null) {
            responseNormalizer.didResolveNull();
          } else {
            responseNormalizer.didResolveScalar(fieldValue);
          }
          break;
        }
      }

      responseNormalizer.didResolve(fieldDescriptor.field, operation.variables());
    }
  }

  private void normalizeObjectField(ObjectFieldDescriptor objectFieldDescriptor, Map<String, Object> objectFieldValues,
      ResponseNormalizer<Map<String, Object>> responseNormalizer) {
    responseNormalizer.willResolveObject(objectFieldDescriptor.field, Optional.fromNullable(objectFieldValues));
    if (objectFieldValues == null) {
      responseNormalizer.didResolveNull();
    } else {
      normalize(operation, responseNormalizer, objectFieldDescriptor.childFields, objectFieldValues);
    }
    responseNormalizer.didResolveObject(objectFieldDescriptor.field, Optional.fromNullable(objectFieldValues));
  }

  private void normalizeObjectListField(ListFieldDescriptor listFieldDescriptor,
      List<Map<String, Object>> listFieldValues, ResponseNormalizer<Map<String, Object>> responseNormalizer) {
    if (listFieldValues == null) {
      responseNormalizer.didResolveNull();
    } else {
      for (int i = 0; i < listFieldDescriptor.items.size(); i++) {
        responseNormalizer.willResolveElement(i);
        responseNormalizer.willResolveObject(listFieldDescriptor.field, Optional.fromNullable(listFieldValues.get(i)));
        normalize(operation, responseNormalizer, listFieldDescriptor.items.get(i), listFieldValues.get(i));
        responseNormalizer.didResolveObject(listFieldDescriptor.field, Optional.fromNullable(listFieldValues.get(i)));
        responseNormalizer.didResolveElement(i);
      }
      responseNormalizer.didResolveList(listFieldValues);
    }
  }

  private void normalizeScalarList(List listFieldValues, ResponseNormalizer<Map<String, Object>> responseNormalizer) {
    if (listFieldValues == null) {
      responseNormalizer.didResolveNull();
    } else {
      for (int i = 0; i < listFieldValues.size(); i++) {
        responseNormalizer.willResolveElement(i);
        responseNormalizer.didResolveScalar(listFieldValues.get(i));
        responseNormalizer.didResolveElement(i);
      }
      responseNormalizer.didResolveList(listFieldValues);
    }
  }

  private static void checkFieldValue(ResponseField field, Object value) {
    if (!field.optional() && value == null) {
      throw new NullPointerException(String.format("Mandatory response field `%s` resolved with null value",
          field.responseName()));
    }
  }

  @SuppressWarnings("unchecked") private static final class ListItemWriter implements ResponseWriter.ListItemWriter {
    final Operation operation;
    final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
    final List<Map<String, FieldDescriptor>> fieldDescriptors = new ArrayList();
    final List fieldValues = new ArrayList();

    ListItemWriter(Operation operation, Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
      this.operation = operation;
      this.customTypeAdapters = customTypeAdapters;
    }

    @Override public void writeString(String value) throws IOException {
      fieldValues.add(value);
    }

    @Override public void writeInt(Integer value) throws IOException {
      fieldValues.add(value);
    }

    @Override public void writeLong(Long value) throws IOException {
      fieldValues.add(value);
    }

    @Override public void writeDouble(Double value) throws IOException {
      fieldValues.add(value);
    }

    @Override public void writeBoolean(Boolean value) throws IOException {
      fieldValues.add(value);
    }

    @Override public void writeCustom(ScalarType scalarType, Object value) throws IOException {
      CustomTypeAdapter typeAdapter = customTypeAdapters.get(scalarType);
      if (typeAdapter == null) {
        fieldValues.add(value);
      } else {
        fieldValues.add(typeAdapter.encode(value));
      }
    }

    @Override public void writeObject(ResponseFieldMarshaller marshaller) throws IOException {
      CacheResponseWriter nestedResponseWriter = new CacheResponseWriter(operation, customTypeAdapters);
      marshaller.marshal(nestedResponseWriter);

      fieldDescriptors.add(nestedResponseWriter.fieldDescriptors);
      fieldValues.add(nestedResponseWriter.fieldValues);
    }
  }

  private static class FieldDescriptor {
    final ResponseField field;

    FieldDescriptor(ResponseField field) {
      this.field = field;
    }
  }

  private static final class ObjectFieldDescriptor extends FieldDescriptor {
    final Map<String, FieldDescriptor> childFields;

    ObjectFieldDescriptor(ResponseField field, Map<String, FieldDescriptor> childFields) {
      super(field);
      this.childFields = childFields;
    }
  }

  private static final class ListFieldDescriptor extends FieldDescriptor {
    final List<Map<String, FieldDescriptor>> items;

    ListFieldDescriptor(ResponseField field, List<Map<String, FieldDescriptor>> items) {
      super(field);
      this.items = items;
    }
  }
}
