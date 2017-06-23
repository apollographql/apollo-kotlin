package com.apollographql.apollo.api.graphql;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.google.common.truth.Truth.assertThat;

public class CacheKeyForFieldTest {

  enum Episode {
    JEDI
  }

  @Test
  public void testFieldWithNoArguments() {
    ResponseField field = ResponseField.forString("hero", "hero", null, false);
    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero");
  }

  @Test
  public void testFieldWithNoArgumentsWithAlias() {
    ResponseField field = ResponseField.forString("r2", "hero", null, false);
    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero");
  }

  @Test
  public void testFieldWithArgument() {
    //noinspection unchecked
    ResponseField field = ResponseField.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .build(), false);

    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero(episode:JEDI)");
  }

  @Test
  public void testFieldWithArgumentAndAlias() {
    //noinspection unchecked
    ResponseField field = ResponseField.forString("r2", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .build(), false);

    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero(episode:JEDI)");
  }

  @Test
  public void testFieldWithVariableArgument() {
    //noinspection unchecked
    ResponseField field = ResponseField.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", new UnmodifiableMapBuilder<String, Object>(2)
            .put("kind", "Variable")
            .put("variableName", "episode")
            .build())
        .build(), false);

    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("episode", Episode.JEDI);
        return map;
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero(episode:JEDI)");
  }

  @Test
  public void testFieldWithVariableArgumentNull() {
    //noinspection unchecked
    ResponseField field = ResponseField.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", new UnmodifiableMapBuilder<String, Object>(2)
            .put("kind", "Variable")
            .put("variableName", "episode")
            .build())
        .build(), false);

    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("episode", null);
        return map;
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero(episode:null)");
  }

  @Test
  public void testFieldWithMultipleArgument() {
    //noinspection unchecked
    ResponseField field = ResponseField.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .put("color", "blue")
        .build(), false);

    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero(color:blue,episode:JEDI)");
  }

  @Test
  public void testFieldWithMultipleArgumentsOrderIndependent() {
    //noinspection unchecked
    ResponseField field = ResponseField.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .put("color", "blue")
        .build(), false);

    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };

    //noinspection unchecked
    ResponseField fieldTwo = ResponseField.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("color", "blue")
        .put("episode", "JEDI")
        .build(), false);

    assertThat(fieldTwo.cacheKey(variables)).isEqualTo(field.cacheKey(variables));
  }

  @Test
  public void testFieldWithNestedObject() {
    //noinspection unchecked
    ResponseField field = ResponseField.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .put("nested", new UnmodifiableMapBuilder<String, Object>(2)
            .put("foo", 1)
            .put("bar", 2)
            .build())
        .build(), false);

    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero(episode:JEDI,nested:[bar:2,foo:1])");
  }

  @Test
  public void testFieldWithNestedObjectAndVariables() {
    //noinspection unchecked
    ResponseField field = ResponseField.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .put("nested", new UnmodifiableMapBuilder<String, Object>(2)
            .put("foo", new UnmodifiableMapBuilder<String, Object>(2)
                .put("kind", "Variable")
                .put("variableName", "stars")
                .build())
            .put("bar", "2")
            .build())
        .build(), false);

    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("stars", 1);
        return map;
      }
    };
    assertThat(field.cacheKey(variables)).isEqualTo("hero(episode:JEDI,nested:[bar:2,foo:1])");
  }

}
