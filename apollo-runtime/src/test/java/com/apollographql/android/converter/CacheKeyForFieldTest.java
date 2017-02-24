package com.apollographql.android.converter;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.util.UnmodifiableMapBuilder;

import junit.framework.Assert;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

public class CacheKeyForFieldTest {

  enum Episode {
    JEDI
  }

  @Test
  public void testFieldWithNoArguments() {
    Field field = Field.forString("hero", "hero", null, false);
    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    Assert.assertEquals("hero", field.cacheKey(variables));
  }

  @Test
  public void testFieldWithNoArgumentsWithAlias() {
    Field field = Field.forString("r2", "hero", null, false);
    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    Assert.assertEquals("hero", field.cacheKey(variables));
  }

  @Test
  public void testFieldWithArgument() {
    //noinspection unchecked
    Field field = Field.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .build(), false);

    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    Assert.assertEquals("hero(episode:JEDI)", field.cacheKey(variables));
  }

  @Test
  public void testFieldWithArgumentAndAlias() {
    //noinspection unchecked
    Field field = Field.forString("r2", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .build(), false);

    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    Assert.assertEquals("hero(episode:JEDI)", field.cacheKey(variables));
  }

  @Test
  public void testFieldWithVariableArgument() {
    //noinspection unchecked
    Field field = Field.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
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
    Assert.assertEquals("hero(episode:JEDI)", field.cacheKey(variables));
  }

  @Test
  public void testFieldWithMultipleArgument() {
    //noinspection unchecked
    Field field = Field.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .put("color", "blue")
        .build(), false);

    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };
    Assert.assertEquals("hero(color:blue,episode:JEDI)", field.cacheKey(variables));
  }

  @Test
  public void testFieldWithMultipleArgumentsOrderIndependent() {
    //noinspection unchecked
    Field field = Field.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .put("color", "blue")
        .build(), false);

    Operation.Variables variables = new Operation.Variables() {
      @Nonnull @Override public Map<String, Object> valueMap() {
        return super.valueMap();
      }
    };

    //noinspection unchecked
    Field fieldTwo = Field.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("color", "blue")
        .put("episode", "JEDI")
        .build(), false);

    Assert.assertEquals(fieldTwo.cacheKey(variables), field.cacheKey(variables));
  }

  @Test
  public void testFieldWithNestedObject() {
    //noinspection unchecked
    Field field = Field.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
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
    Assert.assertEquals("hero(episode:JEDI,nested:[bar:2,foo:1])", field.cacheKey(variables));
  }

  @Test
  public void testFieldWithNestedObjectAndVariables() {
    //noinspection unchecked
    Field field = Field.forString("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
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
    Assert.assertEquals("hero(episode:JEDI,nested:[bar:2,foo:1])", field.cacheKey(variables));
  }

}
