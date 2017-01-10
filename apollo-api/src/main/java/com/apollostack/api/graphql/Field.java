package com.apollostack.api.graphql;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public final class Field {
  public static final int TYPE_STRING = 1;
  public static final int TYPE_INT = 2;
  public static final int TYPE_LONG = 3;
  public static final int TYPE_DOUBLE = 4;
  public static final int TYPE_BOOL = 5;
  public static final int TYPE_OBJECT = 6;
  public static final int TYPE_LIST = 7;

  private final int type;
  private final String responseName;
  private final String fieldName;
  private final Map<String, Object> arguments;
  private final NestedReader nestedReader;
  private final boolean optional;

  public static Field forOptionalString(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    return new Field(TYPE_STRING, responseName, fieldName, arguments, null, true);
  }

  public static Field forString(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    return new Field(TYPE_STRING, responseName, fieldName, arguments, null, false);
  }

  public static Field forOptionalInt(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    return new Field(TYPE_INT, responseName, fieldName, arguments, null, true);
  }

  public static Field forInt(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    return new Field(TYPE_INT, responseName, fieldName, arguments, null, false);
  }

  public static Field forOptionalLong(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    return new Field(TYPE_LONG, responseName, fieldName, arguments, null, true);
  }

  public static Field forLong(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    return new Field(TYPE_LONG, responseName, fieldName, arguments, null, false);
  }

  public static Field forOptionalDouble(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    return new Field(TYPE_DOUBLE, responseName, fieldName, arguments, null, true);
  }

  public static Field forDouble(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    return new Field(TYPE_DOUBLE, responseName, fieldName, arguments, null, false);
  }

  public static Field forOptionalBool(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    return new Field(TYPE_BOOL, responseName, fieldName, arguments, null, true);
  }

  public static Field forBool(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    return new Field(TYPE_BOOL, responseName, fieldName, arguments, null, false);
  }

  public static <T> Field forOptionalObject(String responseName, String fieldName, Map<String, Object> arguments,
      NestedReader<T> nestedReader) throws IOException {
    return new Field(TYPE_OBJECT, responseName, fieldName, arguments, nestedReader, true);
  }

  public static <T> Field forObject(String responseName, String fieldName, Map<String, Object> arguments,
      NestedReader<T> nestedReader) throws IOException {
    return new Field(TYPE_OBJECT, responseName, fieldName, arguments, nestedReader, false);
  }

  public static <T> Field forOptionalList(String responseName, String fieldName, Map<String, Object> arguments,
      NestedReader<T> nestedReader) throws IOException {
    return new Field(TYPE_LIST, responseName, fieldName, arguments, nestedReader, true);
  }

  public static <T> Field forList(String responseName, String fieldName, Map<String, Object> arguments,
      NestedReader<T> nestedReader) throws IOException {
    return new Field(TYPE_LIST, responseName, fieldName, arguments, nestedReader, false);
  }

  private Field(int type, String responseName, String fieldName, Map<String, Object> arguments,
      NestedReader nestedReader, boolean optional) {
    this.type = type;
    this.responseName = responseName;
    this.fieldName = fieldName;
    this.arguments = arguments == null ? Collections.<String, Object>emptyMap()
        : Collections.unmodifiableMap(arguments);
    this.nestedReader = nestedReader;
    this.optional = optional;
  }

  public int type() {
    return type;
  }

  public String responseName() {
    return responseName;
  }

  public String fieldName() {
    return fieldName;
  }

  public Map<String, Object> arguments() {
    return arguments;
  }

  public NestedReader nestedReader() {
    return nestedReader;
  }

  public boolean optional() {
    return optional;
  }

  public interface NestedReader<T> {
    T read(ResponseReader reader) throws IOException;
  }
}
