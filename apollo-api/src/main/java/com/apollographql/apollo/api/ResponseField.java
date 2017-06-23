package com.apollographql.apollo.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static java.util.Collections.unmodifiableList;

/**
 * Field is an abstraction for a field in a graphQL operation.
 * Field can refer to: <b>GraphQL Scalar Types, Objects or List</b>. For a complete list of types that a Field
 * object can refer to see {@link ResponseField.Type} class.
 */
public class ResponseField {
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
  public static ResponseField forString(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new ResponseField(Type.STRING, responseName, fieldName, arguments, optional);
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
  public static ResponseField forInt(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new ResponseField(Type.INT, responseName, fieldName, arguments, optional);
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
  public static ResponseField forLong(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new ResponseField(Type.LONG, responseName, fieldName, arguments, optional);
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
  public static ResponseField forDouble(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new ResponseField(Type.DOUBLE, responseName, fieldName, arguments, optional);
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
  public static ResponseField forBoolean(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new ResponseField(Type.BOOLEAN, responseName, fieldName, arguments, optional);
  }

  /**
   * Factory method for creating a Field instance representing {@link Type#ENUM}.
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @return Field instance representing {@link Type#ENUM}
   */
  public static ResponseField forEnum(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new ResponseField(Type.ENUM, responseName, fieldName, arguments, optional);
  }

  /**
   * Factory method for creating a Field instance representing a custom {@link Type#OBJECT}.
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @return Field instance representing custom {@link Type#OBJECT}
   */
  public static ResponseField forObject(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new ResponseField(Type.OBJECT, responseName, fieldName, arguments, optional);
  }

  /**
   * Factory method for creating a Field instance representing {@link Type#SCALAR_LIST}.
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @return Field instance representing {@link Type#SCALAR_LIST}
   */
  public static ResponseField forScalarList(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new ResponseField(Type.SCALAR_LIST, responseName, fieldName, arguments, optional);
  }

  /**
   * Factory method for creating a Field instance representing {@link Type#CUSTOM_LIST}.
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @return Field instance representing {@link Type#CUSTOM_LIST}
   */
  public static ResponseField forCustomList(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new ResponseField(Type.CUSTOM_LIST, responseName, fieldName, arguments, optional);
  }

  /**
   * Factory method for creating a Field instance representing {@link Type#OBJECT_LIST}.
   *
   * @param responseName alias for the result of a field
   * @param fieldName    name of the field in the GraphQL operation
   * @param arguments    arguments to be passed along with the field
   * @param optional     whether the arguments passed along are optional or required
   * @return Field instance representing {@link Type#OBJECT_LIST}
   */
  public static ResponseField forObjectList(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
    return new ResponseField(Type.OBJECT_LIST, responseName, fieldName, arguments, optional);
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
  public static ResponseField forCustomType(String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional, ScalarType scalarType) {
    return new CustomTypeField(responseName, fieldName, arguments, optional, scalarType);
  }

  /**
   * Factory method for creating a Field instance representing {@link Type#FRAGMENT}.
   *
   * @param responseName     alias for the result of a field
   * @param fieldName        name of the field in the GraphQL operation
   * @param conditionalTypes conditional GraphQL types
   * @return Field instance representing {@link Type#FRAGMENT}
   */
  public static ResponseField forFragment(String responseName, String fieldName, List<String> conditionalTypes) {
    return new ConditionalTypeField(Type.FRAGMENT, responseName, fieldName, conditionalTypes);
  }

  /**
   * Factory method for creating a Field instance representing {@link Type#INLINE_FRAGMENT}.
   *
   * @param responseName     alias for the result of a field
   * @param fieldName        name of the field in the GraphQL operation
   * @param conditionalTypes conditional GraphQL types
   * @return Field instance representing {@link Type#INLINE_FRAGMENT}
   */
  public static ResponseField forInlineFragment(String responseName, String fieldName, List<String> conditionalTypes) {
    return new ConditionalTypeField(Type.INLINE_FRAGMENT, responseName, fieldName, conditionalTypes);
  }

  private ResponseField(Type type, String responseName, String fieldName, Map<String, Object> arguments,
      boolean optional) {
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

  /**
   * Resolve field argument value by name. If argument represents a references to the variable, it will be
   * resolved from provided operation variables values.
   *
   * @param name      argument name
   * @param variables values of operation variables
   * @return resolved argument value
   */
  @SuppressWarnings("unchecked") @Nullable public Object resolveArgument(@Nonnull String name,
      @Nonnull Operation.Variables variables) {
    checkNotNull(name, "name == null");
    checkNotNull(variables, "variables == null");
    Map<String, Object> variableValues = variables.valueMap();
    Object argumentValue = arguments.get(name);
    if (argumentValue instanceof Map) {
      Map<String, Object> argumentValueMap = (Map<String, Object>) argumentValue;
      if (isArgumentValueVariableType(argumentValueMap)) {
        String variableName = argumentValueMap.get(VARIABLE_NAME_KEY).toString();
        return variableValues.get(variableName);
      } else {
        return null;
      }
    }
    return argumentValue;
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
    ENUM,
    OBJECT,
    SCALAR_LIST,
    CUSTOM_LIST,
    OBJECT_LIST,
    CUSTOM,
    FRAGMENT,
    INLINE_FRAGMENT
  }

  /**
   * Abstraction for a Field representing a custom GraphQL scalar type.
   */
  public static final class CustomTypeField extends ResponseField {
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
   * Abstraction for a Field representing a custom GraphQL scalar type.
   */
  public static final class ConditionalTypeField extends ResponseField {
    private final List<String> conditionalTypes;

    ConditionalTypeField(Type type, String responseName, String fieldName, List<String> conditionalTypes) {
      super(type, responseName, fieldName, Collections.<String, Object>emptyMap(), false);
      this.conditionalTypes = conditionalTypes != null ? unmodifiableList(conditionalTypes)
          : Collections.<String>emptyList();
    }

    public List<String> conditionalTypes() {
      return conditionalTypes;
    }
  }
}
