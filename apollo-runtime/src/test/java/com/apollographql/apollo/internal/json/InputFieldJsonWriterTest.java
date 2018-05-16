package com.apollographql.apollo.internal.json;

import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.response.CustomTypeValue;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import okio.Buffer;

import static com.google.common.truth.Truth.assertThat;

public class InputFieldJsonWriterTest {
  private Buffer jsonBuffer;
  private JsonWriter jsonWriter;
  private InputFieldJsonWriter inputFieldJsonWriter;

  @Before
  public void setUp() throws IOException {
    jsonBuffer = new Buffer();
    jsonWriter = JsonWriter.of(jsonBuffer);
    jsonWriter.setSerializeNulls(true);
    jsonWriter.beginObject();

    inputFieldJsonWriter = new InputFieldJsonWriter(jsonWriter,
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap()));
  }

  @Test
  public void writeString() throws IOException {
    inputFieldJsonWriter.writeString("someField", "someValue");
    inputFieldJsonWriter.writeString("someNullField", null);
    assertThat(jsonBuffer.readUtf8()).isEqualTo("{\"someField\":\"someValue\",\"someNullField\":null");
  }

  @Test
  public void writeInt() throws IOException {
    inputFieldJsonWriter.writeInt("someField", 1);
    inputFieldJsonWriter.writeInt("someNullField", null);
    assertThat(jsonBuffer.readUtf8()).isEqualTo("{\"someField\":1,\"someNullField\":null");
  }

  @Test
  public void writeLong() throws IOException {
    inputFieldJsonWriter.writeLong("someField", 10L);
    inputFieldJsonWriter.writeLong("someNullField", null);
    assertThat(jsonBuffer.readUtf8()).isEqualTo("{\"someField\":10,\"someNullField\":null");
  }

  @Test
  public void writeDouble() throws IOException {
    inputFieldJsonWriter.writeDouble("someField", 1.01);
    inputFieldJsonWriter.writeDouble("someNullField", null);
    assertThat(jsonBuffer.readUtf8()).isEqualTo("{\"someField\":1.01,\"someNullField\":null");
  }

  @Test
  public void writeNumber() throws IOException {
    inputFieldJsonWriter.writeNumber("someField", BigDecimal.valueOf(1.001));
    inputFieldJsonWriter.writeNumber("someNullField", null);
    assertThat(jsonBuffer.readUtf8()).isEqualTo("{\"someField\":1.001,\"someNullField\":null");
  }

  @Test
  public void writeBoolean() throws IOException {
    inputFieldJsonWriter.writeBoolean("someField", true);
    inputFieldJsonWriter.writeBoolean("someNullField", null);
    assertThat(jsonBuffer.readUtf8()).isEqualTo("{\"someField\":true,\"someNullField\":null");
  }

  @Test
  public void writeObject() throws IOException {
    inputFieldJsonWriter.writeObject("someField", new InputFieldMarshaller() {
      @Override public void marshal(InputFieldWriter writer) throws IOException {
        writer.writeString("someField", "someValue");
      }
    });
    inputFieldJsonWriter.writeObject("someNullField", null);
    assertThat(jsonBuffer.readUtf8()).isEqualTo("{\"someField\":{\"someField\":\"someValue\"},\"someNullField\":null");
  }

  @Test
  public void writeList() throws IOException {
    inputFieldJsonWriter.writeList("someField", new InputFieldWriter.ListWriter() {
      @Override public void write(@NotNull InputFieldWriter.ListItemWriter listItemWriter) throws IOException {
        listItemWriter.writeString("someValue");
      }
    });
    inputFieldJsonWriter.writeList("someNullField", null);
    assertThat(jsonBuffer.readUtf8()).isEqualTo("{\"someField\":[\"someValue\"],\"someNullField\":null");
  }

  @Test
  public void writeCustomBoolean() throws IOException {
    Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new HashMap<>();
    customTypeAdapters.put(new MockCustomScalarType(CustomTypeValue.GraphQLBoolean.class), new MockCustomTypeAdapter() {
      @NotNull @Override public CustomTypeValue encode(@NotNull Object value) {
        return new CustomTypeValue.GraphQLBoolean((Boolean) value);
      }
    });
    inputFieldJsonWriter = new InputFieldJsonWriter(jsonWriter, new ScalarTypeAdapters(customTypeAdapters));
    inputFieldJsonWriter.writeCustom("someField", new MockCustomScalarType(CustomTypeValue.GraphQLBoolean.class), true);
    inputFieldJsonWriter.writeCustom("someNullField", new MockCustomScalarType(CustomTypeValue.GraphQLBoolean.class), null);
    assertThat(jsonBuffer.readUtf8()).isEqualTo("{\"someField\":true,\"someNullField\":null");
  }

  @Test
  public void writeCustomNumber() throws IOException {
    Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new HashMap<>();
    customTypeAdapters.put(new MockCustomScalarType(CustomTypeValue.GraphQLNumber.class), new MockCustomTypeAdapter() {
      @NotNull @Override public CustomTypeValue encode(@NotNull Object value) {
        return new CustomTypeValue.GraphQLNumber((Number) value);
      }
    });
    inputFieldJsonWriter = new InputFieldJsonWriter(jsonWriter, new ScalarTypeAdapters(customTypeAdapters));
    inputFieldJsonWriter.writeCustom("someField", new MockCustomScalarType(CustomTypeValue.GraphQLNumber.class), BigDecimal.valueOf(100.1));
    inputFieldJsonWriter.writeCustom("someNullField", new MockCustomScalarType(CustomTypeValue.GraphQLNumber.class), null);
    assertThat(jsonBuffer.readUtf8()).isEqualTo("{\"someField\":100.1,\"someNullField\":null");
  }

  @Test
  public void writeCustomString() throws IOException {
    Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new HashMap<>();
    customTypeAdapters.put(new MockCustomScalarType(CustomTypeValue.GraphQLString.class), new MockCustomTypeAdapter() {
      @NotNull @Override public CustomTypeValue encode(@NotNull Object value) {
        return new CustomTypeValue.GraphQLString((String) value);
      }
    });
    inputFieldJsonWriter = new InputFieldJsonWriter(jsonWriter, new ScalarTypeAdapters(customTypeAdapters));
    inputFieldJsonWriter.writeCustom("someField", new MockCustomScalarType(CustomTypeValue.GraphQLString.class), "someValue");
    inputFieldJsonWriter.writeCustom("someNullField", new MockCustomScalarType(CustomTypeValue.GraphQLString.class), null);
    assertThat(jsonBuffer.readUtf8()).isEqualTo("{\"someField\":\"someValue\",\"someNullField\":null");
  }

  @Test
  public void writeCustomJsonString() throws IOException {
    Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new HashMap<>();
    customTypeAdapters.put(new MockCustomScalarType(CustomTypeValue.GraphQLJsonString.class), new MockCustomTypeAdapter() {
      @NotNull @Override public CustomTypeValue encode(@NotNull Object value) {
        return new CustomTypeValue.GraphQLJsonString((String) value);
      }
    });
    inputFieldJsonWriter = new InputFieldJsonWriter(jsonWriter, new ScalarTypeAdapters(customTypeAdapters));
    inputFieldJsonWriter.writeCustom("someField", new MockCustomScalarType(CustomTypeValue.GraphQLJsonString.class), "{\"someField\": \"someValue\"}");
    inputFieldJsonWriter.writeCustom("someNullField", new MockCustomScalarType(CustomTypeValue.GraphQLJsonString.class), null);
    assertThat(jsonBuffer.readUtf8()).isEqualTo("{\"someField\":\"{\\\"someField\\\": \\\"someValue\\\"}\",\"someNullField\":null");
  }

  @Test
  public void writeCustomJson() throws IOException {
    Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new HashMap<>();
    customTypeAdapters.put(new MockCustomScalarType(CustomTypeValue.GraphQLJson.class), new MockCustomTypeAdapter() {
      @NotNull @Override public CustomTypeValue encode(@NotNull Object value) {
        return new CustomTypeValue.GraphQLJson((Map<String, Object>) value);
      }
    });
    inputFieldJsonWriter = new InputFieldJsonWriter(jsonWriter, new ScalarTypeAdapters(customTypeAdapters));

    Map<String, Object> objectMap = new LinkedHashMap<>();
    objectMap.put("booleanField", true);
    objectMap.put("stringField", "someValue");
    objectMap.put("numberField", 100);
    objectMap.put("objectField", new UnmodifiableMapBuilder().put("someField", "someValue").build());

    inputFieldJsonWriter.writeCustom("someField", new MockCustomScalarType(CustomTypeValue.GraphQLJson.class), objectMap);
    inputFieldJsonWriter.writeCustom("someNullField", new MockCustomScalarType(CustomTypeValue.GraphQLJson.class), null);
    assertThat(jsonBuffer.readUtf8()).isEqualTo("{\"someField\":{\"booleanField\":true,\"stringField\":\"someValue\",\"numberField\":100,\"objectField\":{\"someField\":\"someValue\"}},\"someNullField\":null");
  }

  private class MockCustomScalarType implements ScalarType {
    final Class clazz;

    MockCustomScalarType(Class clazz) {
      this.clazz = clazz;
    }

    @Override public String typeName() {
      throw new UnsupportedOperationException();
    }

    @Override public Class javaType() {
      return clazz;
    }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MockCustomScalarType)) return false;

      MockCustomScalarType that = (MockCustomScalarType) o;

      return clazz.equals(that.clazz);
    }

    @Override public int hashCode() {
      return clazz.hashCode();
    }
  }

  private abstract class MockCustomTypeAdapter implements CustomTypeAdapter {
    @Override public Object decode(@NotNull CustomTypeValue value) {
      throw new UnsupportedOperationException();
    }
  }
}
