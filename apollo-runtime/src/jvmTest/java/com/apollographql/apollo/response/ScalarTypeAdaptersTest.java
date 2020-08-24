package com.apollographql.apollo.response;

import com.apollographql.apollo.api.CustomTypeAdapter;
import com.apollographql.apollo.api.CustomTypeValue;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

public class ScalarTypeAdaptersTest {

  @Test
  public void customAdapterTakePrecedentOverDefault() {
    final Map<ScalarType, CustomTypeAdapter<?>> customTypeAdapters = new HashMap<>();
    final CustomTypeAdapter expectedAdapter = Mockito.mock(CustomTypeAdapter.class);
    customTypeAdapters.put(new ScalarType() {
      @Override
      public String typeName() {
        return "String";
      }

      @Override
      public String className() {
        return String.class.getName();
      }
    }, expectedAdapter);

    final CustomTypeAdapter actualAdapter = new ScalarTypeAdapters(customTypeAdapters).adapterFor(new ScalarType() {
      @Override
      public String typeName() {
        return "String";
      }

      @Override
      public String className() {
        return String.class.getName();
      }
    });

    assertThat(actualAdapter).isEqualTo(expectedAdapter);
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingAdapter() {
    new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter<?>>emptyMap())
        .adapterFor(
            new ScalarType() {
              @Override
              public String typeName() {
                return "RuntimeException";
              }

              @Override
              public String className() {
                return RuntimeException.class.getName();
              }
            }
        );
  }

  @Test
  public void defaultStringAdapter() {
    final CustomTypeAdapter<String> adapter = defaultAdapter(String.class);
    assertThat(adapter.decode(CustomTypeValue.fromRawValue("string"))).isEqualTo("string");
    assertThat(adapter.encode("string").value).isEqualTo("string");
  }

  @Test
  public void defaultBooleanAdapter() {
    final CustomTypeAdapter<Boolean> adapter = defaultAdapter(Boolean.class);
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(true))).isEqualTo(true);
    assertThat(adapter.encode(true).value).isEqualTo(true);
  }

  @Test
  public void defaultIntegerAdapter() {
    final CustomTypeAdapter<Integer> adapter = defaultAdapter(Integer.class);
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(100))).isEqualTo(100);
    assertThat(adapter.encode(100).value).isEqualTo(100);
  }

  @Test
  public void defaultLongAdapter() {
    final CustomTypeAdapter<Long> adapter = defaultAdapter(Long.class);
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(100L))).isEqualTo(100L);
    assertThat(adapter.encode(100L).value).isEqualTo(100L);
  }

  @Test
  public void defaultFloatAdapter() {
    final CustomTypeAdapter<Float> adapter = defaultAdapter(Float.class);
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(10.10f))).isWithin(0.0f).of(10.10f);
    assertThat(adapter.encode(10.10f).value).isEqualTo(10.10f);
  }

  @Test
  public void defaultDoubleAdapter() {
    final CustomTypeAdapter<Double> adapter = defaultAdapter(Double.class);
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(10.10d))).isWithin(0.0d).of(10.10d);
    assertThat(adapter.encode(10.10d).value).isEqualTo(10.10d);
  }

  @Test
  public void defaultObjectAdapter() {
    final CustomTypeAdapter<Object> adapter = defaultAdapter(Object.class);
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(RuntimeException.class))).isEqualTo("class java.lang.RuntimeException");
    assertThat(adapter.encode(RuntimeException.class).value).isEqualTo("class java.lang.RuntimeException");
  }

  @Test
  public void defaultMapAdapter() {
    final Map<String, Object> value = new UnmodifiableMapBuilder<String, Object>()
        .put("key1", "value1")
        .put("key2", "value2")
        .build();

    final CustomTypeAdapter<Map> adapter = defaultAdapter(Map.class);
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(value))).isEqualTo(value);
    assertThat(adapter.encode(value).value).isEqualTo(value);
  }

  @Test
  public void defaultListAdapter() {
    final List<String> value = Arrays.asList("item 1", "item 2");
    final CustomTypeAdapter<List> adapter = defaultAdapter(List.class);
    assertThat(adapter.decode(CustomTypeValue.fromRawValue(value))).isEqualTo(value);
    assertThat(adapter.encode(value).value).isEqualTo(value);
  }

  @Test
  public void defaultJsonString() {
    final CustomTypeValue.GraphQLJsonObject actualObject = new CustomTypeValue.GraphQLJsonObject(
        new UnmodifiableMapBuilder<String, Object>()
            .put("key", "scalar")
            .put("object", new UnmodifiableMapBuilder<String, Object>()
                .put("nestedKey", "nestedScalar")
                .build()
            )
            .put("list", Arrays.asList("1", "2", "3"))
            .build()
    );
    final String expectedJsonString = "{\"key\":\"scalar\",\"object\":{\"nestedKey\":\"nestedScalar\"},\"list\":[\"1\",\"2\",\"3\"]}";

    final CustomTypeAdapter<String> adapter = defaultAdapter(String.class);
    assertThat(adapter.decode(actualObject)).isEqualTo(expectedJsonString);
  }

  private <T> CustomTypeAdapter<T> defaultAdapter(final Class<T> clazz) {
    return new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter<?>>emptyMap()).adapterFor(
        new ScalarType() {
          @Override
          public String typeName() {
            return clazz.getSimpleName();
          }

          @Override
          public String className() {
            return clazz.getName();
          }
        }
    );
  }
}
