package com.apollographql.android.api.graphql;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class Field {
  private final Type type;
  private final String responseName;
  private final String fieldName;
  private final Map<String, Object> arguments;
  private final boolean optional;

  public static Field forString(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new Field(Type.STRING, responseName, fieldName, arguments, optional);
  }

  public static Field forInt(String responseName, String fieldName, Map<String, Object> arguments, boolean optional) {
    return new Field(Type.INT, responseName, fieldName, arguments, optional);
  }

  public static <T> Field forLong(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new Field(Type.LONG, responseName, fieldName, arguments, optional);
  }

  public static Field forDouble(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new Field(Type.DOUBLE, responseName, fieldName, arguments, optional);
  }

  public static Field forBoolean(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new Field(Type.BOOLEAN, responseName, fieldName, arguments, optional);
  }

  public static <T> Field forObject(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, ObjectReader<T> objectReader) {
    return new ObjectField(responseName, fieldName, arguments, optional, objectReader);
  }

  public static <T> Field forList(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, ListReader<T> listReader) {
    return new ScalarListField(responseName, fieldName, arguments, optional, listReader);
  }

  public static <T> Field forList(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, ObjectReader<T> objectReader) {
    return new ObjectListField(responseName, fieldName, arguments, optional, objectReader);
  }

  public static <T> Field forCustomType(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, ScalarType scalarType) {
    return new CustomTypeField(responseName, fieldName, arguments, optional, scalarType);
  }

  public static <T> Field forConditionalType(String responseName, String fieldName,
      ConditionalTypeReader<T> conditionalTypeReader) {
    return new ConditionalTypeField(responseName, fieldName, conditionalTypeReader);
  }

  private Field(Type type, String responseName, String fieldName, Map<String, Object> arguments, boolean optional) {
    this.type = type;
    this.responseName = responseName;
    this.fieldName = fieldName;
    this.arguments = arguments == null ? Collections.<String, Object>emptyMap()
        : Collections.unmodifiableMap(arguments);
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

  public boolean optional() {
    return optional;
  }

  public static enum Type {
    STRING,
    INT,
    LONG,
    DOUBLE,
    BOOLEAN,
    OBJECT,
    SCALAR_LIST,
    OBJECT_LIST,
    CUSTOM,
    CONDITIONAL
  }

  public interface ObjectReader<T> {
    T read(ResponseReader reader) throws IOException;
  }

  public interface ListReader<T> {
    T read(ListItemReader reader) throws IOException;
  }

  public interface ConditionalTypeReader<T> {
    T read(String conditionalType, ResponseReader reader) throws IOException;
  }

  public interface ListItemReader {

    String readString() throws IOException;

    Integer readInt() throws IOException;

    Long readLong() throws IOException;

    Double readDouble() throws IOException;

    Boolean readBoolean() throws IOException;

    <T> T readCustomType(ScalarType scalarType) throws IOException;
  }

  public static final class ObjectField extends Field {
    private final ObjectReader objectReader;

    ObjectField(String responseName, String fieldName, Map<String, Object> arguments, boolean optional,
        ObjectReader objectReader) {
      super(Type.OBJECT, responseName, fieldName, arguments, optional);
      this.objectReader = objectReader;
    }

    public ObjectReader objectReader() {
      return objectReader;
    }
  }

  public static final class ScalarListField extends Field {
    private final ListReader listReader;

    ScalarListField(String responseName, String fieldName, Map<String, Object> arguments, boolean optional,
        ListReader listReader) {
      super(Type.SCALAR_LIST, responseName, fieldName, arguments, optional);
      this.listReader = listReader;
    }

    public ListReader listReader() {
      return listReader;
    }
  }

  public static final class ObjectListField extends Field {
    private final ObjectReader objectReader;

    ObjectListField(String responseName, String fieldName, Map<String, Object> arguments, boolean optional,
        ObjectReader objectReader) {
      super(Type.OBJECT_LIST, responseName, fieldName, arguments, optional);
      this.objectReader = objectReader;
    }

    public ObjectReader objectReader() {
      return objectReader;
    }
  }

  public static final class CustomTypeField extends Field {
    private final ScalarType scalarType;

    CustomTypeField(String responseName, String fieldName, Map<String, Object> arguments, boolean optional,
        ScalarType scalarType) {
      super(Type.CUSTOM, responseName, fieldName, arguments, optional);
      this.scalarType = scalarType;
    }

    public ScalarType scalarType() {
      return scalarType;
    }
  }

  public static final class ConditionalTypeField extends Field {
    private final ConditionalTypeReader conditionalTypeReader;

    ConditionalTypeField(String responseName, String fieldName, ConditionalTypeReader conditionalTypeReader) {
      super(Type.CONDITIONAL, responseName, fieldName, null, false);
      this.conditionalTypeReader = conditionalTypeReader;
    }

    public ConditionalTypeReader conditionalTypeReader() {
      return conditionalTypeReader;
    }
  }
}
