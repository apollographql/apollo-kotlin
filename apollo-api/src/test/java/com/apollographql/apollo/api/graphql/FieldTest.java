package com.apollographql.apollo.api.graphql;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.google.common.truth.Truth.assertThat;

public class FieldTest {

  @SuppressWarnings("unchecked") @Test public void resolvedArguments() {
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

    Map<String, Object> resolvedArguments = field.resolvedArguments(variables);
    assertThat(resolvedArguments.get("episode")).isEqualTo("JEDI");
    assertThat(resolvedArguments.get("nested")).isNotNull();
    assertThat(((Map<String, Object>) resolvedArguments.get("nested")).get("foo")).isEqualTo(1);
  }
}
