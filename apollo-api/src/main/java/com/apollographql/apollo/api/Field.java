package com.apollographql.apollo.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Field is an abstraction for a field in a graphQL operation. For example, in the following graphQL query, Field
 * represents abstraction for field 'name':
 *
 * <pre> {@code
 *  {
 *      hero {
 *        name
 *      }
 *  }
 *      }
 * </pre>
 *
 * Field can refer to: <b>GraphQL Scalar Types, Objects or List</b>. For a complete list of types that a Field
 * object can refer to see {@link Field.Type} class.
 */
public class Field {
  private final Type type;
  private final String responseName;
  private final String fieldName;
  private final Map<String, Object> arguments;
  private final boolean optional;

  private static final String VARIABLE_IDENTIFIER_KEY = "kind";
  private static final String VARIABLE_IDENTIFIER_VALUE = "Variable";
  private static final String VARIABLE_NAME_KEY = "variableName";

  /**
   * Factory method for creating a Field instance representing {@link Type#STRING}.
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @return Field instance representing {@link Type#STRING}
   */
  public static Field forString(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new Field(Type.STRING, responseName, fieldName, arguments, optional);
  }

  /**
   * Factory method for creating a Field instance representing {@link Type#INT}.
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @return Field instance representing {@link Type#INT}
   */
  public static Field forInt(String responseName, String fieldName, Map<String, Object> arguments, boolean optional) {
    return new Field(Type.INT, responseName, fieldName, arguments, optional);
  }

  /**
   * Factory method for creating a Field instance representing {@link Type#LONG}.
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @return Field instance representing {@link Type#LONG}
   */
  public static Field forLong(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new Field(Type.LONG, responseName, fieldName, arguments, optional);
  }

  /**
   * Factory method for creating a Field instance representing {@link Type#DOUBLE}.
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @return Field instance representing {@link Type#DOUBLE}
   */
  public static Field forDouble(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new Field(Type.DOUBLE, responseName, fieldName, arguments, optional);
  }

  /**
   * Factory method for creating a Field instance representing {@link Type#BOOLEAN}.
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @return Field instance representing {@link Type#BOOLEAN}
   */
  public static Field forBoolean(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new Field(Type.BOOLEAN, responseName, fieldName, arguments, optional);
  }

  /**
   * Factory method for creating a Field instance representing a custom {@link Type#OBJECT}.
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @param objectReader converts the field response to the custom object type
   * @param <T>          type of the custom object
   * @return Field instance representing custom {@link Type#OBJECT}
   */
  public static <T> Field forObject(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, ObjectReader<T> objectReader) {
    return new ObjectField(responseName, fieldName, arguments, optional, objectReader);
  }

  /**
   * Factory method for creating a Field instance representing {@link Type#SCALAR_LIST}.
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @param listReader   converts the field response to a list of GraphQL scalar types
   * @param <T>          type of the scalar type
   * @return Field instance representing {@link Type#SCALAR_LIST}
   */
  public static <T> Field forList(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, ListReader<T> listReader) {
    return new ScalarListField(responseName, fieldName, arguments, optional, listReader);
  }

  /**
   * Factory method for creating a Field instance representing {@link Type#OBJECT_LIST}.
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @param objectReader converts the field response to a list of custom object types
   * @param <T>          type of the custom object
   * @return Field instance representing {@link Type#OBJECT_LIST}
   */
  public static <T> Field forList(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, ObjectReader<T> objectReader) {
    return new ObjectListField(responseName, fieldName, arguments, optional, objectReader);
  }

  /**
   * Factory method for creating a Field instance representing a custom GraphQL Scalar type, {@link Type#CUSTOM}
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @param scalarType   the custom scalar type of the field
   * @return Field instance representing {@link Type#CUSTOM}
   */
  public static Field forCustomType(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, ScalarType scalarType) {
    return new CustomTypeField(responseName, fieldName, arguments, optional, scalarType);
  }

  /**
   * Factory method for creating a Field instance representing {@link Type#CONDITIONAL}.
   *
   * @param responseName          alias for the result of a field
   * @param fieldName             name of the field in the GraphQL operation
   * @param conditionalTypeReader converts the field response to an optional type
   * @param <T>                   type of the conditional
   * @return Field instance representing {@link Type#CONDITIONAL}
   */
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

  public String cacheKey(Operation.Variables variables) {
    if (arguments.isEmpty()) {
      return fieldName();
    }
    return String.format("%s(%s)", fieldName(), orderIndependentKey(arguments, variables));
  }

  private String orderIndependentKey(Map<String, Object> objectMap, Operation.Variables variables) {
    if (isArgumentValueVariableType(objectMap)) {
      return orderIndependentKeyForVariableArgument(objectMap, variables);
    }
    List<Map.Entry<String, Object>> sortedArguments = new ArrayList<>(objectMap.entrySet());
    Collections.sort(sortedArguments, new Comparator<Map.Entry<String, Object>>() {
      @Override public int compare(Map.Entry<String, Object> argumentOne, Map.Entry<String, Object> argumentTwo) {
        return argumentOne.getKey().compareTo(argumentTwo.getKey());
      }
    });
    StringBuilder independentKey = new StringBuilder();
    for (int i = 0; i < sortedArguments.size(); i++) {
      Map.Entry<String, Object> argument = sortedArguments.get(i);
      if (argument.getValue() instanceof Map) {
        //noinspection unchecked
        final Map<String, Object> objectArg = (Map<String, Object>) argument.getValue();
        boolean isArgumentVariable = isArgumentValueVariableType(objectArg);
        independentKey
            .append(argument.getKey())
            .append(":")
            .append(isArgumentVariable ? "" : "[")
            .append(orderIndependentKey(objectArg, variables))
            .append(isArgumentVariable ? "" : "]");
      } else {
        independentKey.append(argument.getKey())
            .append(":")
            .append(argument.getValue().toString());
      }
      if (i < sortedArguments.size() - 1) {
        independentKey.append(",");
      }
    }
    return independentKey.toString();
  }

  private boolean isArgumentValueVariableType(Map<String, Object> objectMap) {
    return objectMap.containsKey(VARIABLE_IDENTIFIER_KEY)
        && objectMap.get(VARIABLE_IDENTIFIER_KEY).equals(VARIABLE_IDENTIFIER_VALUE)
        && objectMap.containsKey(VARIABLE_NAME_KEY);
  }

  private String orderIndependentKeyForVariableArgument(Map<String, Object> objectMap, Operation.Variables variables) {
    Object variable = objectMap.get(VARIABLE_NAME_KEY);
    //noinspection SuspiciousMethodCalls
    Object resolvedVariable = variables.valueMap().get(variable);
    if (resolvedVariable == null) {
      return null;
    } else if (resolvedVariable instanceof Map) {
      //noinspection unchecked
      return orderIndependentKey((Map<String, Object>) resolvedVariable, variables);
    } else {
      return resolvedVariable.toString();
    }
  }

  /**
   * An abstraction for the field types
   */
  public enum Type {
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

  /**
   * Abstraction for a Field representing a custom Object type.
   */
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

  /**
   * Abstraction for a Field representing a list of GraphQL scalar types.
   */
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

  /**
   * Abstraction for a Field representing a list of custom Objects.
   */
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

  /**
   * Abstraction for a Field representing a custom GraphQL scalar type.
   */
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

  /**
   * Abstraction for a Field representing a conditional type. Conditional Type is used for parsing inline fragments or
   * fragments. Here is an example of how it is used:
   * <pre>
   * {@code
   * final Field[] fields = {
   *            Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
   *
   *                @Override
   *                public Fragments read(String conditionalType, ResponseReader reader) throws IOException { return
   *                      fragmentsFieldMapper.map(reader, conditionalType);
   *                      }
   *                 })
   *           };
   * }
   * </pre>
   *
   * In the example above, the first field '__typename' will be read and then passed to another nested mapper along with
   * reader that will decide by checking conditionalType what type of fragment it will parse.
   */
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
