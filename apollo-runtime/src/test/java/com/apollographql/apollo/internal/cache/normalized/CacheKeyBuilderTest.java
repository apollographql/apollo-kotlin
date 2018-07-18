package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.InputType;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

public class CacheKeyBuilderTest {
  private final CacheKeyBuilder cacheKeyBuilder = new RealCacheKeyBuilder();

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
    assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero");
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
    assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero");
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
    assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})");
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
    assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})");
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
    assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})");
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
    assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":null})");
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
    assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"color\":\"blue\",\"episode\":\"JEDI\"})");
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

    assertThat(cacheKeyBuilder.build(fieldTwo, variables)).isEqualTo(cacheKeyBuilder.build(field, variables));
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
    assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\",\"nested\":{\"bar\":2,\"foo\":1}})");
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
    assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\"})");
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
    assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo("hero({\"episode\":\"JEDI\",\"nested\":{\"bar\":\"2\",\"foo\":1}})");
  }


  @Test
  public void testFieldWithNestedObjectAndInputTypes() {
    //noinspection unchecked
    Map<String, Object> arguments = new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "JEDI")
        .put("nested", new UnmodifiableMapBuilder<String, Object>(2)
            .put("foo", new UnmodifiableMapBuilder<String, Object>(2)
                .put("kind", "Variable")
                .put("variableName", "testInput")
                .build())
            .put("bar", "2")
            .build())
        .build();
    ResponseField field = createResponseField("hero", "hero", arguments);

    final InputType testInput = new InputType() {
      @NotNull @Override public InputFieldMarshaller marshaller() {
        return new InputFieldMarshaller() {
          @Override public void marshal(InputFieldWriter writer) throws IOException {
            writer.writeString("string", "string");
            writer.writeInt("int", 1);
            writer.writeLong("long", 2L);
            writer.writeDouble("double", 3D);
            writer.writeNumber("number", BigDecimal.valueOf(4));
            writer.writeBoolean("boolean", true);
            writer.writeCustom("custom", new ScalarType() {
              @Override public String typeName() {
                return "EPISODE";
              }

              @Override public Class javaType() {
                return String.class;
              }
            }, "JEDI");
            writer.writeObject("object", new InputFieldMarshaller() {
              @Override public void marshal(InputFieldWriter writer) throws IOException {
                writer.writeString("string", "string");
                writer.writeInt("int", 1);
              }
            });
            writer.writeList("list", new InputFieldWriter.ListWriter() {
              @Override
              public void write(@NotNull InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
                listItemWriter.writeString("string");
                listItemWriter.writeInt(1);
                listItemWriter.writeLong(2L);
                listItemWriter.writeDouble(3D);
                listItemWriter.writeNumber(BigDecimal.valueOf(4));
                listItemWriter.writeBoolean(true);
                listItemWriter.writeCustom(new ScalarType() {
                  @Override public String typeName() {
                    return "EPISODE";
                  }

                  @Override public Class javaType() {
                    return String.class;
                  }
                }, "JEDI");
                listItemWriter.writeObject(new InputFieldMarshaller() {
                  @Override public void marshal(InputFieldWriter writer) throws IOException {
                    writer.writeString("string", "string");
                    writer.writeInt("int", 1);
                  }
                });
                listItemWriter.writeList(new InputFieldWriter.ListWriter() {
                  @Override
                  public void write(@NotNull InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
                    listItemWriter.writeString("string");
                    listItemWriter.writeInt(1);
                  }
                });
              }
            });
            writer.writeString("null", null);
          }
        };
      }
    };

    Operation.Variables variables = new Operation.Variables() {
      @NotNull @Override public Map<String, Object> valueMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("testInput", testInput);
        return map;
      }
    };
    assertThat(cacheKeyBuilder.build(field, variables)).isEqualTo(
        "hero({\"episode\":\"JEDI\",\"nested\":{\"bar\":\"2\",\"foo\":{\"boolean\":true,\"custom\":\"JEDI\",\"double\":3.0,\"int\":1,\"list\":[\"string\",1,2,3.0,4,true,\"JEDI\",{\"int\":1,\"string\":\"string\"},[\"string\",1]],\"long\":2,\"null\":null,\"number\":4,\"object\":{\"int\":1,\"string\":\"string\"},\"string\":\"string\"}}})");
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
