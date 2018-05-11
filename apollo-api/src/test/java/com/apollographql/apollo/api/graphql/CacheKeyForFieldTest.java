package com.apollographql.apollo.api.graphql;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import static com.google.common.truth.Truth.assertThat;

public class CacheKeyForFieldTest {

  enum Episode {
    JEDI
  }

  @Test
  public void testFieldWithNoArguments() {
    ResponseField field = ResponseField.forString("hero", "hero", null, false,
        Collections.<ResponseField.Condition>emptyList());
    Operation.Variables variables = new Operation.Variables() {
      @NotNull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero");
  }

  @Test
  public void testFieldWithNoArgumentsWithAlias() {
    ResponseField field = ResponseField.forString("r2", "hero", null, false,
        Collections.<ResponseField.Condition>emptyList());
    Operation.Variables variables = new Operation.Variables() {
      @NotNull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero");
  }

  @Test
  public void testFieldWithArgument() {
    //noinspection unchecked
    Map<String, Object> arguments = new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .build();
    ResponseField field = createResponseField("hero", "hero", arguments);

    Operation.Variables variables = new Operation.Variables() {
      @NotNull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero({\"episode\":\"JEDI\"})");
  }

  @Test
  public void testFieldWithArgumentAndAlias() {
    //noinspection unchecked
    Map<String, Object> arguments = new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .build();
    ResponseField field = createResponseField("r2", "hero", arguments);

    Operation.Variables variables = new Operation.Variables() {
      @NotNull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero({\"episode\":\"JEDI\"})");
  }

  @Test
  public void testFieldWithVariableArgument() {
    //noinspection unchecked
    UnmodifiableMapBuilder<String, Object> argument = new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", new UnmodifiableMapBuilder<String, Object>(2)
            .put("kind", "Variable")
            .put("variableName", "episode")
            .build());
    ResponseField field = createResponseField("hero", "hero", argument
        .build());

    Operation.Variables variables = new Operation.Variables() {
      @NotNull @Override public Map<String, Object> valueMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("episode", Episode.JEDI);
        return map;
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero({\"episode\":\"JEDI\"})");
  }

  @Test
  public void testFieldWithVariableArgumentNull() {
    //noinspection unchecked
    UnmodifiableMapBuilder<String, Object> argument = new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", new UnmodifiableMapBuilder<String, Object>(2)
            .put("kind", "Variable")
            .put("variableName", "episode")
            .build());
    ResponseField field = createResponseField("hero", "hero", argument
        .build());

    Operation.Variables variables = new Operation.Variables() {
      @NotNull @Override public Map<String, Object> valueMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("episode", null);
        return map;
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero({\"episode\":null})");
  }

  @Test
  public void testFieldWithMultipleArgument() {
    //noinspection unchecked
    Map<String, Object> build = new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .put("color", "blue")
        .build();
    ResponseField field = createResponseField("hero", "hero", build);

    Operation.Variables variables = new Operation.Variables() {
      @NotNull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero({\"color\":\"blue\",\"episode\":\"JEDI\"})");
  }

  @Test
  public void testFieldWithMultipleArgumentsOrderIndependent() {
    //noinspection unchecked
    Map<String, Object> arguments = new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .put("color", "blue")
        .build();
    ResponseField field = createResponseField("hero", "hero", arguments);

    Operation.Variables variables = new Operation.Variables() {
      @NotNull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };

    //noinspection unchecked
    Map<String, Object> fieldTwoArguments = new UnmodifiableMapBuilder<String, Object>(1)
        .put("color", "blue")
        .put("episode", "JEDI")
        .build();
    ResponseField fieldTwo = createResponseField("hero", "hero", fieldTwoArguments);

    assertThat(fieldTwo.cacheKey(variables)).isEqualTo(field.cacheKey(variables));
  }

  @Test
  public void testFieldWithNestedObject() {
    //noinspection unchecked
    Map<String, Object> arguments = new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .put("nested", new UnmodifiableMapBuilder<String, Object>(2)
            .put("foo", 1)
            .put("bar", 2)
            .build())
        .build();
    ResponseField field = createResponseField("hero", "hero", arguments);

    Operation.Variables variables = new Operation.Variables() {
      @NotNull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero({\"episode\":\"JEDI\",\"nested\":{\"bar\":2,\"foo\":1}})");
  }

  @Test
  public void testFieldWithNonPrimitiveValue() {
    //noinspection unchecked
    ResponseField field = ResponseField.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", Episode.JEDI)
        .build(), false, Collections.<ResponseField.Condition>emptyList());

    Operation.Variables variables = new Operation.Variables() {
      @NotNull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero({\"episode\":\"JEDI\"})");
  }

  @Test
  public void testFieldWithNestedObjectAndVariables() {
    //noinspection unchecked
    Map<String, Object> arguments = new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .put("nested", new UnmodifiableMapBuilder<String, Object>(2)
            .put("foo", new UnmodifiableMapBuilder<String, Object>(2)
                .put("kind", "Variable")
                .put("variableName", "stars")
                .build())
            .put("bar", "2")
            .build())
        .build();
    ResponseField field = createResponseField("hero", "hero", arguments);

    Operation.Variables variables = new Operation.Variables() {
      @NotNull @Override public Map<String, Object> valueMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("stars", 1);
        return map;
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo(
        "hero({\"episode\":\"JEDI\",\"nested\":{\"bar\":\"2\",\"foo\":1}})");
  }

  private ResponseField createResponseField(String responseName, String fieldName) {
    return createResponseField(responseName, fieldName, null);
  }

  private ResponseField createResponseField(String responseName, String fieldName, Map<String, Object> arguments) {
    return ResponseField.forString(
        responseName,
        fieldName,
        arguments,
        false,
        Collections.<ResponseField.Condition>emptyList());
  }
}
