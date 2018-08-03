package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.api.InputType;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.internal.json.JsonWriter;
import com.apollographql.apollo.internal.json.SortedInputFieldMapWriter;
import com.apollographql.apollo.internal.json.Utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import okio.Buffer;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public class RealCacheKeyBuilder implements CacheKeyBuilder {
  private final Comparator<String> argumentNameComparator = new Comparator<String>() {
    @Override public int compare(String first, String second) {
      return first.compareTo(second);
    }
  };

  @NotNull @Override
  public String build(@NotNull ResponseField field, @NotNull Operation.Variables variables) {
    checkNotNull(field, "field == null");
    checkNotNull(variables, "variables == null");

    if (field.arguments().isEmpty()) {
      return field.fieldName();
    }

    Object resolvedArguments = resolveArguments(field.arguments(), variables);
    try {
      Buffer buffer = new Buffer();
      JsonWriter jsonWriter = JsonWriter.of(buffer);
      jsonWriter.setSerializeNulls(true);
      Utils.writeToJson(resolvedArguments, jsonWriter);
      jsonWriter.close();
      return String.format("%s(%s)", field.fieldName(), buffer.readUtf8());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Map<String, Object> resolveArguments(Map<String, Object> objectMap, Operation.Variables variables) {
    Map<String, Object> result = new TreeMap<>(argumentNameComparator);
    for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
      if (entry.getValue() instanceof Map) {
        Map<String, Object> nestedObjectMap = (Map<String, Object>) entry.getValue();
        if (ResponseField.isArgumentValueVariableType(nestedObjectMap)) {
          result.put(entry.getKey(), resolveVariableArgument(nestedObjectMap, variables));
        } else {
          result.put(entry.getKey(), resolveArguments(nestedObjectMap, variables));
        }
      } else {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  private Object resolveVariableArgument(Map<String, Object> objectMap, Operation.Variables variables) {
    Object variable = objectMap.get(ResponseField.VARIABLE_NAME_KEY);
    //noinspection SuspiciousMethodCalls
    Object resolvedVariable = variables.valueMap().get(variable);
    if (resolvedVariable == null) {
      return null;
    } else if (resolvedVariable instanceof Map) {
      //noinspection unchecked
      return resolveArguments((Map<String, Object>) resolvedVariable, variables);
    } else if (resolvedVariable instanceof InputType) {
      try {
        SortedInputFieldMapWriter inputFieldMapWriter = new SortedInputFieldMapWriter(argumentNameComparator);
        ((InputType) resolvedVariable).marshaller().marshal(inputFieldMapWriter);
        return resolveArguments(inputFieldMapWriter.map(), variables);
      } catch (IOException e) {
        // should never happen
        throw new RuntimeException(e);
      }
    } else {
      return resolvedVariable;
    }
  }
}
