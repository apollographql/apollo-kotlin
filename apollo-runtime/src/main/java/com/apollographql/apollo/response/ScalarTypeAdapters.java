package com.apollographql.apollo.response;

import com.apollographql.apollo.api.ScalarType;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class ScalarTypeAdapters {
  private static final Map<Class, CustomTypeAdapter> DEFAULT_ADAPTERS = defaultAdapters();
  private final Map<ScalarType, CustomTypeAdapter> customAdapters;

  public ScalarTypeAdapters(@Nonnull Map<ScalarType, CustomTypeAdapter> customAdapters) {
    this.customAdapters = checkNotNull(customAdapters, "customAdapters == null");
  }

  @SuppressWarnings("unchecked") @Nonnull public <T> CustomTypeAdapter<T> adapterFor(@Nonnull ScalarType scalarType) {
    checkNotNull(scalarType, "scalarType == null");

    CustomTypeAdapter<T> customTypeAdapter = customAdapters.get(scalarType);
    if (customTypeAdapter == null) {
      customTypeAdapter = DEFAULT_ADAPTERS.get(scalarType.javaType());
    }

    if (customTypeAdapter == null) {
      throw new IllegalArgumentException(String.format("Can't map GraphQL type: %s to: %s. Did you forget to add "
          + "custom type adapter?", scalarType.typeName(), scalarType.javaType()));
    }

    return customTypeAdapter;
  }

  private static Map<Class, CustomTypeAdapter> defaultAdapters() {
    Map<Class, CustomTypeAdapter> adapters = new LinkedHashMap<>();
    adapters.put(String.class, new DefaultCustomTypeAdapter<String>() {
      @Nonnull @Override public String decode(@Nonnull CustomTypeValue value) {
        return value.value.toString();
      }
    });
    adapters.put(Boolean.class, new DefaultCustomTypeAdapter<Boolean>() {
      @Nonnull @Override public Boolean decode(@Nonnull CustomTypeValue value) {
        if (value instanceof CustomTypeValue.GraphQLBoolean) {
          return (Boolean) value.value;
        } else if (value instanceof CustomTypeValue.GraphQLString) {
          return Boolean.parseBoolean(((CustomTypeValue.GraphQLString) value).value);
        } else {
          throw new IllegalArgumentException("Can't map: " + value + " to Boolean");
        }
      }
    });
    adapters.put(Integer.class, new DefaultCustomTypeAdapter<Integer>() {
      @Nonnull @Override public Integer decode(@Nonnull CustomTypeValue value) {
        if (value instanceof CustomTypeValue.GraphQLNumber) {
          return ((Number) value.value).intValue();
        } else if (value instanceof CustomTypeValue.GraphQLString) {
          return Integer.parseInt(((CustomTypeValue.GraphQLString) value).value);
        } else {
          throw new IllegalArgumentException("Can't map: " + value + " to Integer");
        }
      }
    });
    adapters.put(Long.class, new DefaultCustomTypeAdapter<Long>() {
      @Nonnull @Override public Long decode(@Nonnull CustomTypeValue value) {
        if (value instanceof CustomTypeValue.GraphQLNumber) {
          return ((Number) value.value).longValue();
        } else if (value instanceof CustomTypeValue.GraphQLString) {
          return Long.parseLong(((CustomTypeValue.GraphQLString) value).value);
        } else {
          throw new IllegalArgumentException("Can't map: " + value + " to Long");
        }
      }
    });
    adapters.put(Float.class, new DefaultCustomTypeAdapter<Float>() {
      @Nonnull @Override public Float decode(@Nonnull CustomTypeValue value) {
        if (value instanceof CustomTypeValue.GraphQLNumber) {
          return ((Number) value.value).floatValue();
        } else if (value instanceof CustomTypeValue.GraphQLString) {
          return Float.parseFloat(((CustomTypeValue.GraphQLString) value).value);
        } else {
          throw new IllegalArgumentException("Can't map: " + value + " to Float");
        }
      }
    });
    adapters.put(Double.class, new DefaultCustomTypeAdapter<Double>() {
      @Nonnull @Override public Double decode(@Nonnull CustomTypeValue value) {
        if (value instanceof CustomTypeValue.GraphQLNumber) {
          return ((Number) value.value).doubleValue();
        } else if (value instanceof CustomTypeValue.GraphQLString) {
          return Double.parseDouble(((CustomTypeValue.GraphQLString) value).value);
        } else {
          throw new IllegalArgumentException("Can't map: " + value + " to Double");
        }
      }
    });
    return adapters;
  }

  private abstract static class DefaultCustomTypeAdapter<T> implements CustomTypeAdapter<T> {
    @Nonnull @Override public CustomTypeValue encode(@Nonnull T value) {
      return CustomTypeValue.fromRawValue(value);
    }
  }
}
