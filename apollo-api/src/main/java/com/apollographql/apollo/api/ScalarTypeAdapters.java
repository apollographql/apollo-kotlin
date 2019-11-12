package com.apollographql.apollo.api;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class ScalarTypeAdapters {
  private static final Map<Class, CustomTypeAdapter> DEFAULT_ADAPTERS = defaultAdapters();
  private final Map<String, CustomTypeAdapter> customAdapters;

  public ScalarTypeAdapters(@NotNull Map<ScalarType, CustomTypeAdapter> customAdapters) {
    Map<ScalarType, CustomTypeAdapter> nonNullcustomAdapters = checkNotNull(customAdapters, "customAdapters == null");
    this.customAdapters = new HashMap<>();
    for (Map.Entry<ScalarType, CustomTypeAdapter> entry : nonNullcustomAdapters.entrySet()) {
      this.customAdapters.put(entry.getKey().typeName(), entry.getValue());
    }
  }

  @SuppressWarnings("unchecked") @NotNull public <T> CustomTypeAdapter<T> adapterFor(@NotNull ScalarType scalarType) {
    checkNotNull(scalarType, "scalarType == null");

    CustomTypeAdapter<T> customTypeAdapter = customAdapters.get(scalarType.typeName());
    if (customTypeAdapter == null) {
      customTypeAdapter = DEFAULT_ADAPTERS.get(scalarType.javaType());
    }

    if (customTypeAdapter == null) {
      throw new IllegalArgumentException(String.format("Can't map GraphQL type: %s to: %s. Did you forget to add "
          + "a custom type adapter?", scalarType.typeName(), scalarType.javaType()));
    }

    return customTypeAdapter;
  }

  private static Map<Class, CustomTypeAdapter> defaultAdapters() {
    Map<Class, CustomTypeAdapter> adapters = new LinkedHashMap<>();
    adapters.put(String.class, new DefaultCustomTypeAdapter<String>() {
      @NotNull @Override public String decode(@NotNull CustomTypeValue value) {
        return value.value.toString();
      }
    });
    adapters.put(Boolean.class, new DefaultCustomTypeAdapter<Boolean>() {
      @NotNull @Override public Boolean decode(@NotNull CustomTypeValue value) {
        if (value instanceof CustomTypeValue.GraphQLBoolean) {
          return (Boolean) value.value;
        } else if (value instanceof CustomTypeValue.GraphQLString) {
          return Boolean.parseBoolean(((CustomTypeValue.GraphQLString) value).value);
        } else {
          throw new IllegalArgumentException("Can't decode: " + value + " into Boolean");
        }
      }
    });
    adapters.put(Integer.class, new DefaultCustomTypeAdapter<Integer>() {
      @NotNull @Override public Integer decode(@NotNull CustomTypeValue value) {
        if (value instanceof CustomTypeValue.GraphQLNumber) {
          return ((Number) value.value).intValue();
        } else if (value instanceof CustomTypeValue.GraphQLString) {
          return Integer.parseInt(((CustomTypeValue.GraphQLString) value).value);
        } else {
          throw new IllegalArgumentException("Can't decode: " + value + " into Integer");
        }
      }
    });
    adapters.put(Long.class, new DefaultCustomTypeAdapter<Long>() {
      @NotNull @Override public Long decode(@NotNull CustomTypeValue value) {
        if (value instanceof CustomTypeValue.GraphQLNumber) {
          return ((Number) value.value).longValue();
        } else if (value instanceof CustomTypeValue.GraphQLString) {
          return Long.parseLong(((CustomTypeValue.GraphQLString) value).value);
        } else {
          throw new IllegalArgumentException("Can't decode: " + value + " into Long");
        }
      }
    });
    adapters.put(Float.class, new DefaultCustomTypeAdapter<Float>() {
      @NotNull @Override public Float decode(@NotNull CustomTypeValue value) {
        if (value instanceof CustomTypeValue.GraphQLNumber) {
          return ((Number) value.value).floatValue();
        } else if (value instanceof CustomTypeValue.GraphQLString) {
          return Float.parseFloat(((CustomTypeValue.GraphQLString) value).value);
        } else {
          throw new IllegalArgumentException("Can't decode: " + value + " into Float");
        }
      }
    });
    adapters.put(Double.class, new DefaultCustomTypeAdapter<Double>() {
      @NotNull @Override public Double decode(@NotNull CustomTypeValue value) {
        if (value instanceof CustomTypeValue.GraphQLNumber) {
          return ((Number) value.value).doubleValue();
        } else if (value instanceof CustomTypeValue.GraphQLString) {
          return Double.parseDouble(((CustomTypeValue.GraphQLString) value).value);
        } else {
          throw new IllegalArgumentException("Can't decode: " + value + " into Double");
        }
      }
    });
    adapters.put(FileUpload.class, new CustomTypeAdapter<FileUpload>() {
      @Override public FileUpload decode(@NotNull CustomTypeValue value) {
        return null;
      }

      @NotNull
      @Override
      public CustomTypeValue encode(@NotNull FileUpload value) {
        return new CustomTypeValue.GraphQLString(null);
      }
    });
    adapters.put(Object.class, new DefaultCustomTypeAdapter<Object>() {
      @NotNull @Override public Object decode(@NotNull CustomTypeValue value) {
        return value.value;
      }
    });
    adapters.put(Map.class, new DefaultCustomTypeAdapter<Map>() {
      @NotNull @Override public Map decode(@NotNull CustomTypeValue value) {
        if (value instanceof CustomTypeValue.GraphQLJsonObject) {
          return ((CustomTypeValue.GraphQLJsonObject) value).value;
        } else {
          throw new IllegalArgumentException("Can't decode: " + value + " into Map");
        }
      }
    });
    adapters.put(List.class, new DefaultCustomTypeAdapter<List>() {
      @NotNull @Override public List decode(@NotNull CustomTypeValue value) {
        if (value instanceof CustomTypeValue.GraphQLJsonList) {
          return ((CustomTypeValue.GraphQLJsonList) value).value;
        } else {
          throw new IllegalArgumentException("Can't decode: " + value + " into List");
        }
      }
    });
    return adapters;
  }

  private abstract static class DefaultCustomTypeAdapter<T> implements CustomTypeAdapter<T> {
    @NotNull @Override public CustomTypeValue encode(@NotNull T value) {
      return CustomTypeValue.fromRawValue(value);
    }
  }
}
