package com.apollographql.apollo.response;

import com.apollographql.apollo.internal.json.Utils;

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

  public static CustomTypeValue fromRawValue(Object value) {
    if (value instanceof Map || value instanceof List) {
      try {
        return new GraphQLJsonString(Utils.toJsonString(value));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
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
   * Represents a {@code JsonString} value
   */
  public static class GraphQLJsonString extends CustomTypeValue<String> {
    public GraphQLJsonString(String value) {
      super(value);
    }
  }

  /**
   * Represents a JSON object value, will serialized as an regular json object
   */
  public static class GraphQLJson extends CustomTypeValue<Map<String, Object>> {
    public GraphQLJson(Map<String, Object> value) {
      super(value);
    }
  }
}
