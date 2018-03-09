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
    } else if (value instanceof java.lang.Boolean) {
      return new GraphQLBoolean((java.lang.Boolean) value);
    } else if (value instanceof java.lang.Number) {
      return new GraphQLNumber((java.lang.Number) value);
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
  public static class GraphQLString extends CustomTypeValue<java.lang.String> {
    public GraphQLString(java.lang.String value) {
      super(value);
    }
  }

  /**
   * Represents a {@code Boolean} value
   */
  public static class GraphQLBoolean extends CustomTypeValue<java.lang.Boolean> {
    public GraphQLBoolean(java.lang.Boolean value) {
      super(value);
    }
  }

  /**
   * Represents a {@code Number} value
   */
  public static class GraphQLNumber extends CustomTypeValue<java.lang.Number> {
    public GraphQLNumber(java.lang.Number value) {
      super(value);
    }
  }

  /**
   * Represents a {@code JsonString} value
   */
  public static class GraphQLJsonString extends CustomTypeValue<java.lang.String> {
    public GraphQLJsonString(java.lang.String value) {
      super(value);
    }
  }
}
