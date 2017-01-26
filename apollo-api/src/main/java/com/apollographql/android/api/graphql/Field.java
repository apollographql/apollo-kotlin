package com.apollographql.android.api.graphql;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/** TODO */
public final class Field {
  private final Type type;
  private final String responseName;
  private final String fieldName;
  private final Map<String, Object> arguments;
  private final ObjectReader objectReader;
  private final ListReader listReader;
  private final boolean optional;
  private final TypeMapping typeMapping;

  public static Field forString(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new Field(Type.STRING, responseName, fieldName, arguments, null, null, optional, null);
  }

  public static Field forInt(String responseName, String fieldName, Map<String, Object> arguments, boolean optional) {
    return new Field(Type.INT, responseName, fieldName, arguments, null, null, optional, null);
  }

  public static <T> Field forLong(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new Field(Type.LONG, responseName, fieldName, arguments, null, null, optional, null);
  }

  public static Field forDouble(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new Field(Type.DOUBLE, responseName, fieldName, arguments, null, null, optional, null);
  }

  public static Field forBoolean(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new Field(Type.BOOLEAN, responseName, fieldName, arguments, null, null, optional, null);
  }

  public static <T> Field forObject(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, ObjectReader<T> objectReader) {
    return new Field(Type.OBJECT, responseName, fieldName, arguments, objectReader, null, optional, null);
  }

  public static <T> Field forList(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, ListReader<T> listReader) {
    return new Field(Type.LIST, responseName, fieldName, arguments, null, listReader, optional, null);
  }

  public static <T> Field forList(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, ObjectReader<T> objectReader) {
    return new Field(Type.LIST, responseName, fieldName, arguments, objectReader, null, optional, null);
  }

  public static <T> Field forCustomType(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, TypeMapping typeMapping) {
    return new Field(Type.CUSTOM, responseName, fieldName, arguments, null, null, optional, typeMapping);
  }

  private Field(Type type, String responseName, String fieldName, Map<String, Object> arguments,
      ObjectReader objectReader, ListReader listReader, boolean optional, TypeMapping typeMapping) {
    this.type = type;
    this.responseName = responseName;
    this.fieldName = fieldName;
    this.arguments = arguments == null ? Collections.<String, Object>emptyMap()
        : Collections.unmodifiableMap(arguments);
    this.objectReader = objectReader;
    this.listReader = listReader;
    this.optional = optional;
    this.typeMapping = typeMapping;
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

  public ObjectReader objectReader() {
    return objectReader;
  }

  public boolean optional() {
    return optional;
  }

  public ListReader listReader() {
    return listReader;
  }

  public TypeMapping typeMapping() {
    return typeMapping;
  }

  public static enum Type {
    STRING,
    INT,
    LONG,
    DOUBLE,
    BOOLEAN,
    OBJECT,
    LIST,
    CUSTOM
  }

  public interface ObjectReader<T> {
    T read(ResponseReader reader) throws IOException;
  }

  public interface ListReader<T> {
    T read(ListItemReader reader) throws IOException;
  }

  public interface ListItemReader {

    String readString() throws IOException;

    Integer readInt() throws IOException;

    Long readLong() throws IOException;

    Double readDouble() throws IOException;

    Boolean readBoolean() throws IOException;

    <T> T readCustomType(TypeMapping typeMapping) throws IOException;
  }
}
