package com.apollostack.api.graphql;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public final class Field {
  private final Type type;
  private final String responseName;
  private final String fieldName;
  private final Map<String, Object> arguments;
  private final NestedReader nestedReader;
  private final ListItemReader listItemReader;
  private final boolean optional;

  public static Field forString(String responseName, String fieldName, Map<String, Object> arguments, boolean optional)
      throws IOException {
    return new Field(Type.STRING, responseName, fieldName, arguments, null, null, optional);
  }

  public static Field forInt(String responseName, String fieldName, Map<String, Object> arguments, boolean optional)
      throws IOException {
    return new Field(Type.INT, responseName, fieldName, arguments, null, null, optional);
  }

  public static <T> Field forLong(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) throws IOException {
    return new Field(Type.LONG, responseName, fieldName, arguments, null, null, optional);
  }

  public static Field forDouble(String responseName, String fieldName, Map<String, Object> arguments, boolean optional)
      throws IOException {
    return new Field(Type.DOUBLE, responseName, fieldName, arguments, null, null, optional);
  }

  public static Field forBoolean(String responseName, String fieldName, Map<String, Object> arguments, boolean optional)
      throws IOException {
    return new Field(Type.BOOLEAN, responseName, fieldName, arguments, null, null, optional);
  }

  public static <T> Field forObject(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, NestedReader<T> nestedReader) throws IOException {
    return new Field(Type.OBJECT, responseName, fieldName, arguments, nestedReader, null, optional);
  }

  public static <T> Field forList(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, ListItemReader<T> listItemReader) throws IOException {
    return new Field(Type.LIST, responseName, fieldName, arguments, null, listItemReader, optional);
  }

  private Field(Type type, String responseName, String fieldName, Map<String, Object> arguments,
      NestedReader nestedReader, ListItemReader listItemReader, boolean optional) {
    this.type = type;
    this.responseName = responseName;
    this.fieldName = fieldName;
    this.arguments = arguments == null ? Collections.<String, Object>emptyMap()
        : Collections.unmodifiableMap(arguments);
    this.nestedReader = nestedReader;
    this.listItemReader = listItemReader;
    this.optional = optional;
  }

  public Type type() {
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

  public ListItemReader listItemReader() {
    return listItemReader;
  }

  public static enum Type {
    STRING,
    INT,
    LONG,
    DOUBLE,
    BOOLEAN,
    OBJECT,
    LIST
  }

  public interface NestedReader<T> {
    T read(ResponseReader reader) throws IOException;
  }

  public interface ListItemReader<T> {
    T read(ResponseReader.ListItemReader reader) throws IOException;
  }
}
