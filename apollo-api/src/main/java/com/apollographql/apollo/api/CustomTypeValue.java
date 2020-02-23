package com.apollographql.apollo.api;

import java.util.List;
import java.util.Map;

/**
 * A wrapper class for representation of custom GraphQL type value, used in user provided {@link CustomTypeAdapter}
 * encoding / decoding functions.
 *
 * @param <T> value type, one of the supported types <ul><li>{@link java.lang.String} <li>{@link
 *            java.lang.Boolean}<li>{@link java.lang.Number}<li>{@link Map}<li>{@link List}<ul/>
 */
public abstract class CustomTypeValue<T> {
  public final T value;

  @SuppressWarnings("unchecked")
  public static CustomTypeValue fromRawValue(Object value) {
    if (value instanceof Map) {
      return new GraphQLJsonObject((Map<String, Object>) value);
    } else if (value instanceof List) {
      return new GraphQLJsonList((List<Object>) value);
    } else if (value instanceof Boolean) {
      return new GraphQLBoolean((Boolean) value);
    } else if (value instanceof Number) {
      return new GraphQLNumber((Number) value);
    } else {
      return new GraphQLString(value.toString());
    }
  }

  private CustomTypeValue(T value) {
    this.value = value;
  }

  /**
   * Represents a {@code String} value
   */
  public static class GraphQLString extends CustomTypeValue<String> {
    public GraphQLString(String value) {
      super(value);
    }
  }

  /**
   * Represents a {@code Boolean} value
   */
  public static class GraphQLBoolean extends CustomTypeValue<Boolean> {
    public GraphQLBoolean(Boolean value) {
      super(value);
    }
  }

  /**
   * Represents a {@code Number} value
   */
  public static class GraphQLNumber extends CustomTypeValue<Number> {
    public GraphQLNumber(Number value) {
      super(value);
    }
  }

  /**
   * Represents a JSON object value
   */
  public static class GraphQLJsonObject extends CustomTypeValue<Map<String, Object>> {
    public GraphQLJsonObject(Map<String, Object> value) {
      super(value);
    }
  }

  /**
   * Represents a JSON list value
   */
  public static class GraphQLJsonList extends CustomTypeValue<List<Object>> {
    public GraphQLJsonList(List<Object> value) {
      super(value);
    }
  }
}
