package com.apollographql.apollo.api.internal;

import com.apollographql.apollo.api.CustomTypeAdapter;
import com.apollographql.apollo.api.CustomTypeValue;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import okio.BufferedSource;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.math.BigDecimal;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.fail;

public class SimpleResponseReaderTest {
  private final List<ResponseField.Condition> noConditions = Collections.emptyList();

  @Test public void readString() {
    final ResponseField successField = ResponseField.forString("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forString("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", "response1");
    recordSet.put("successFieldName", "response2");
    recordSet.put("classCastExceptionField", 1);

    final SimpleResponseReader responseReader = responseReader(recordSet);

    assertThat(responseReader.readString(successField)).isEqualTo("response1");
    try {
      responseReader.readString(classCastExceptionField);
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readInt() {
    final ResponseField successField = ResponseField.forInt("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forInt("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", BigDecimal.valueOf(1));
    recordSet.put("successFieldName", BigDecimal.valueOf(2));
    recordSet.put("classCastExceptionField", "anything");

    final SimpleResponseReader responseReader = responseReader(recordSet);

    assertThat(responseReader.readInt(successField)).isEqualTo(1);
    try {
      responseReader.readInt(classCastExceptionField);
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readLong() {
    final ResponseField successField = ResponseField.forLong("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forLong("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", BigDecimal.valueOf(1));
    recordSet.put("successFieldName", BigDecimal.valueOf(2));
    recordSet.put("classCastExceptionField", "anything");

    final SimpleResponseReader responseReader = responseReader(recordSet);

    assertThat(responseReader.readLong(successField)).isEqualTo(1);
    try {
      responseReader.readLong(classCastExceptionField);
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readDouble() {
    final ResponseField successField = ResponseField.forDouble("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forDouble("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", BigDecimal.valueOf(1.1));
    recordSet.put("successFieldName", BigDecimal.valueOf(2.2));
    recordSet.put("classCastExceptionField", "anything");

    final SimpleResponseReader responseReader = responseReader(recordSet);

    assertThat(responseReader.readDouble(successField)).isEqualTo(1.1D);
    try {
      responseReader.readDouble(classCastExceptionField);
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readBoolean() {
    final ResponseField successField = ResponseField.forBoolean("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forBoolean("classCastExceptionField", "classCastExceptionField", null,
        false, noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", true);
    recordSet.put("successFieldName", false);
    recordSet.put("classCastExceptionField", "anything");

    final SimpleResponseReader responseReader = responseReader(recordSet);

    assertThat(responseReader.readBoolean(successField)).isTrue();
    try {
      responseReader.readBoolean(classCastExceptionField);
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readObject() {
    final Object responseObject = new Object();
    final ResponseField successField = ResponseField.forObject("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forObject("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", Collections.emptyMap());
    recordSet.put("successFieldName", Collections.emptyMap());
    recordSet.put("classCastExceptionField", "anything");

    final SimpleResponseReader responseReader = responseReader(recordSet);

    assertThat(responseReader.readObject(successField, new ResponseReader.ObjectReader<Object>() {
      @Override public Object read(ResponseReader reader) {
        return responseObject;
      }
    })).isEqualTo(responseObject);

    try {
      responseReader.readObject(classCastExceptionField, new ResponseReader.ObjectReader<Object>() {
        @Override public Object read(ResponseReader reader) {
          return reader.readString(ResponseField.forString("anything", "anything", null, true, noConditions));
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readFragment() {
    final Object responseObject = new Object();
    final ResponseField successFragmentField = ResponseField.forFragment("__typename", "__typename",
        Collections.<ResponseField.Condition>singletonList(ResponseField.Condition.typeCondition(new String[]{"Fragment1"})));

    final ResponseField skipFragmentField = ResponseField.forFragment("__typename", "__typename",
        Collections.<ResponseField.Condition>singletonList(ResponseField.Condition.typeCondition(new String[]{"Fragment2"})));

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("__typename", "Fragment1");

    final SimpleResponseReader responseReader = responseReader(recordSet);

    assertThat(responseReader.readFragment(successFragmentField, new ResponseReader.ObjectReader<Object>() {
      @Override public Object read(ResponseReader reader) {
        return responseObject;
      }
    })).isEqualTo(responseObject);

    assertThat(responseReader.readFragment(skipFragmentField, new ResponseReader.ObjectReader<Object>() {
      @Override public Object read(ResponseReader reader) {
        return responseObject;
      }
    })).isNull();
  }

  @Test public void readCustom() throws ParseException {
    final ResponseField.CustomTypeField successField = ResponseField.forCustomType("successFieldResponseName", "successFieldName", null,
        false, DATE_CUSTOM_TYPE, noConditions);
    final ResponseField.CustomTypeField classCastExceptionField = ResponseField.forCustomType("classCastExceptionField",
        "classCastExceptionField", null, false, DATE_CUSTOM_TYPE, noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", "2017-04-16");
    recordSet.put("successFieldName", "2018-04-16");
    recordSet.put("classCastExceptionField", 0);

    final SimpleResponseReader responseReader = responseReader(recordSet);

    assertThat(responseReader.<Date>readCustomType(successField)).isEqualTo(DATE_TIME_FORMAT.parse("2017-04-16"));
    try {
      responseReader.readCustomType(classCastExceptionField);
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readCustomObjectMap() {
    final ScalarType mapScalarType = new ScalarType() {
      @Override public String typeName() {
        return Map.class.getName();
      }

      @Override public String className() {
        return Map.class.getName();
      }
    };

    final ResponseField.CustomTypeField successField = ResponseField.forCustomType("successFieldResponseName", "successFieldName", null,
        false, mapScalarType, noConditions);

    final Map<String, Object> objectMap = new HashMap<>();
    objectMap.put("string", "string");
    objectMap.put("boolean", true);
    objectMap.put("double", 1.99D);
    objectMap.put("float", 2.99F);
    objectMap.put("long", 3L);
    objectMap.put("int", 4);
    objectMap.put("stringList", asList("string1", "string2"));
    objectMap.put("booleanList", asList("true", "false"));
    objectMap.put("doubleList", asList(1.99D, 2.99D));
    objectMap.put("floatList", asList(3.99F, 4.99F, 5.99F));
    objectMap.put("longList", asList(5L, 7L));
    objectMap.put("intList", asList(8, 9, 10));
    objectMap.put("object", new HashMap<>(objectMap));
    objectMap.put("objectList", asList(new HashMap<>(objectMap), new HashMap<>(objectMap)));

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", objectMap);
    recordSet.put("successFieldName", objectMap);

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat((Map<?, ?>) responseReader.readCustomType(successField)).containsExactlyEntriesIn(objectMap);
  }

  @Test public void readCustomObjectList() {
    final ScalarType listScalarType = new ScalarType() {
      @Override public String typeName() {
        return List.class.getName();
      }

      @Override public String className() {
        return List.class.getName();
      }
    };
    final ResponseField.CustomTypeField successField = ResponseField.forCustomType("successFieldResponseName", "successFieldName", null,
        false, listScalarType, noConditions);

    final Map<String, Object> objectMap = new HashMap<>();
    objectMap.put("string", "string");
    objectMap.put("boolean", true);

    final List<?> objectList = asList(objectMap, objectMap);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", objectList);
    recordSet.put("successFieldName", objectList);

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat((List<?>) responseReader.readCustomType(successField)).containsExactlyElementsIn(objectList);
  }

  @Test public void readCustomWithDefaultAdapter() {
    final ResponseField.CustomTypeField stringField = ResponseField.forCustomType("stringField", "stringField", null, false,
        scalarTypeFor(String.class), noConditions);
    final ResponseField.CustomTypeField booleanField = ResponseField.forCustomType("booleanField", "booleanField", null, false,
        scalarTypeFor(Boolean.class), noConditions);
    final ResponseField.CustomTypeField integerField = ResponseField.forCustomType("integerField", "integerField", null, false,
        scalarTypeFor(Integer.class), noConditions);
    final ResponseField.CustomTypeField longField = ResponseField.forCustomType("longField", "longField", null, false,
        scalarTypeFor(Long.class), noConditions);
    final ResponseField.CustomTypeField floatField = ResponseField.forCustomType("floatField", "floatField", null, false,
        scalarTypeFor(Float.class), noConditions);
    final ResponseField.CustomTypeField doubleField = ResponseField.forCustomType("doubleField", "doubleField", null, false,
        scalarTypeFor(Double.class), noConditions);
    final ResponseField.CustomTypeField unsupportedField = ResponseField.forCustomType("unsupportedField", "unsupportedField", null, false,
        scalarTypeFor(RuntimeException.class), noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put(stringField.getResponseName(), "string");
    recordSet.put(booleanField.getResponseName(), true);
    recordSet.put(integerField.getResponseName(), BigDecimal.valueOf(1));
    recordSet.put(longField.getResponseName(), BigDecimal.valueOf(2));
    recordSet.put(floatField.getResponseName(), BigDecimal.valueOf(3.99));
    recordSet.put(doubleField.getResponseName(), BigDecimal.valueOf(4.99));
    recordSet.put(unsupportedField.getResponseName(), "smth");

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat(responseReader.<String>readCustomType(stringField)).isEqualTo("string");
    assertThat(responseReader.<Boolean>readCustomType(booleanField)).isEqualTo(true);
    assertThat(responseReader.<Integer>readCustomType(integerField)).isEqualTo(1);
    assertThat(responseReader.<Long>readCustomType(longField)).isEqualTo(2);
    assertThat(responseReader.<Float>readCustomType(floatField)).isWithin(0.0f).of(3.99f);
    assertThat(responseReader.<Double>readCustomType(doubleField)).isWithin(0.0d).of(4.99d);

    try {
      responseReader.readCustomType(unsupportedField);
      fail("Expect IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  @Test public void readStringList() {
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList("value1", "value2", "value3"));
    recordSet.put("successFieldName", asList("value4", "value5"));
    recordSet.put("classCastExceptionField", "anything");

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) {
        return reader.readString();
      }
    })).isEqualTo(asList("value1", "value2", "value3"));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readIntList() {
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
    recordSet.put("successFieldName", asList(BigDecimal.valueOf(4), BigDecimal.valueOf(5)));
    recordSet.put("classCastExceptionField", "anything");

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) {
        return reader.readInt();
      }
    })).isEqualTo(asList(1, 2, 3));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readLongList() {
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
    recordSet.put("successFieldName", asList(BigDecimal.valueOf(4), BigDecimal.valueOf(5)));
    recordSet.put("classCastExceptionField", "anything");

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) {
        return reader.readInt();
      }
    })).isEqualTo(asList(1, 2, 3));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readDoubleList() {
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
    recordSet.put("successFieldName", asList(BigDecimal.valueOf(4), BigDecimal.valueOf(5)));
    recordSet.put("classCastExceptionField", "anything");

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) {
        return reader.readDouble();
      }
    })).isEqualTo(asList(1D, 2D, 3D));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readBooleanList() {
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList(true, false, true));
    recordSet.put("successFieldName", asList(false, false));
    recordSet.put("classCastExceptionField", "anything");

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) {
        return reader.readBoolean();
      }
    })).isEqualTo(asList(true, false, true));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readCustomList() throws ParseException {
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList("2017-04-16", "2017-04-17", "2017-04-18"));
    recordSet.put("successFieldName", asList("2017-04-19", "2017-04-20"));
    recordSet.put("classCastExceptionField", "anything");

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) {
        return reader.readCustomType(DATE_CUSTOM_TYPE);
      }
    })).isEqualTo(asList(DATE_TIME_FORMAT.parse("2017-04-16"), DATE_TIME_FORMAT.parse("2017-04-17"), DATE_TIME_FORMAT.parse("2017-04-18")));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readObjectList() {
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions);

    final Object responseObject1 = new Object();
    final Object responseObject2 = new Object();
    final Object responseObject3 = new Object();
    final List<?> objects = asList(responseObject1, responseObject2, responseObject3);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));
    recordSet.put("successFieldName", asList(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));
    recordSet.put("classCastExceptionField", "anything");

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      int index = 0;

      @Override public Object read(ResponseReader.ListItemReader reader) {
        return reader.readObject(new ResponseReader.ObjectReader<Object>() {
          @Override public Object read(ResponseReader reader) {
            return objects.get(index++);
          }
        });
      }
    })).containsExactlyElementsIn(objects);

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readListOfScalarList() {
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, noConditions);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        noConditions);

    final List<List<String>> response1 = asList(asList("1", "2"), asList("3", "4", "5"));
    final List<List<String>> response2 = asList(asList("6", "7", "8"), asList("9", "0"));

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", response1);
    recordSet.put("successFieldName", response2);
    recordSet.put("classCastExceptionField", "anything");

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader<List<String>>() {
      @Override public List<String> read(ResponseReader.ListItemReader reader) {
        return reader.readList(new ResponseReader.ListReader<String>() {
          @Override public String read(ResponseReader.ListItemReader reader) {
            return reader.readString();
          }
        });
      }
    })).isEqualTo(asList(asList("1", "2"), asList("3", "4", "5")));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void optionalFieldsIOException() {
    final SimpleResponseReader responseReader = responseReader(Collections.<String, Object>emptyMap());
    responseReader.readString(ResponseField.forString("stringField", "stringField", null, true, noConditions));
    responseReader.readInt(ResponseField.forInt("intField", "intField", null, true, noConditions));
    responseReader.readLong(ResponseField.forLong("longField", "longField", null, true, noConditions));
    responseReader.readDouble(ResponseField.forDouble("doubleField", "doubleField", null, true, noConditions));
    responseReader.readBoolean(ResponseField.forBoolean("booleanField", "booleanField", null, true, noConditions));
    responseReader.readObject(ResponseField.forObject("objectField", "objectField", null, true, noConditions),
        new ResponseReader.ObjectReader<Object>() {
          @Override public Object read(ResponseReader reader) {
            return null;
          }
        }
    );
    responseReader.readList(ResponseField.forList("scalarListField", "scalarListField", null, true, noConditions),
        new ResponseReader.ListReader() {
          @Override public Object read(ResponseReader.ListItemReader reader) {
            return null;
          }
        }
    );
    responseReader.readCustomType(ResponseField.forCustomType("customTypeField", "customTypeField", null, true, DATE_CUSTOM_TYPE,
        noConditions));
  }

  @Test public void mandatoryFieldsIOException() {
    final SimpleResponseReader responseReader = responseReader(Collections.<String, Object>emptyMap());

    try {
      responseReader.readString(ResponseField.forString("stringField", "stringField", null, false, noConditions));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readInt(ResponseField.forInt("intField", "intField", null, false, noConditions));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readLong(ResponseField.forLong("longField", "longField", null, false, noConditions));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readDouble(ResponseField.forDouble("doubleField", "doubleField", null, false, noConditions));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readBoolean(ResponseField.forBoolean("booleanField", "booleanField", null, false, noConditions));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readObject(ResponseField.forObject("objectField", "objectField", null, false, noConditions),
          new ResponseReader.ObjectReader<Object>() {
            @Override public Object read(ResponseReader reader) {
              return null;
            }
          }
      );
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readList(ResponseField.forList("scalarListField", "scalarListField", null, false, noConditions),
          new ResponseReader.ListReader() {
            @Override public Object read(ResponseReader.ListItemReader reader) {
              return null;
            }
          }
      );
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readCustomType(ResponseField.forCustomType("customTypeField", "customTypeField", null, false, DATE_CUSTOM_TYPE,
          noConditions));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }
  }

  @Test public void readScalarListWithNulls() {
    final ResponseField scalarList = ResponseField.forList("list", "list", null, false, noConditions);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("list", asList(null, "item2", "item3", null, "item5", null));

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(scalarList, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) {
        return reader.readString();
      }
    })).isEqualTo(asList(null, "item2", "item3", null, "item5", null));
  }

  @Test public void readObjectListWithNulls() {
    final ResponseField listField = ResponseField.forList("list", "list", null, false, noConditions);
    final ResponseField indexField = ResponseField.forList("index", "index", null, false, noConditions);
    final List responseObjects = asList(null, new Object(), new Object(), null, new Object(), null);
    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("list", asList(
        null,
        new UnmodifiableMapBuilder<String, Object>(1)
            .put("index", "1")
            .build(),
        new UnmodifiableMapBuilder<String, Object>(1)
            .put("index", "2")
            .build(),
        null,
        new UnmodifiableMapBuilder<String, Object>(1)
            .put("index", "4")
            .build(),
        null
    ));

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(listField, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) {
        return reader.readObject(new ResponseReader.ObjectReader<Object>() {
          @Override public Object read(ResponseReader reader) {
            return responseObjects.get(Integer.parseInt(reader.readString(indexField)));
          }
        });
      }
    })).isEqualTo(responseObjects);
  }

  private static SimpleResponseReader responseReader(Map<String, Object> recordSet) {
    Map<ScalarType, CustomTypeAdapter<?>> customTypeAdapters = new HashMap<>();
    customTypeAdapters.put(DATE_CUSTOM_TYPE, new CustomTypeAdapter() {
      @Override public Object decode(@NotNull CustomTypeValue value) {
        try {
          return DATE_TIME_FORMAT.parse(value.value.toString());
        } catch (ParseException e) {
          throw new ClassCastException();
        }
      }

      @NotNull @Override public CustomTypeValue encode(@NotNull Object value) {
        throw new UnsupportedOperationException();
      }
    });
    customTypeAdapters.put(URL_CUSTOM_TYPE, new CustomTypeAdapter() {
      @Override public Object decode(@NotNull CustomTypeValue value) {
        throw new UnsupportedOperationException();
      }

      @NotNull @Override public CustomTypeValue encode(@NotNull Object value) {
        throw new UnsupportedOperationException();
      }
    });
    customTypeAdapters.put(OBJECT_CUSTOM_TYPE, new CustomTypeAdapter() {
      @Override public Object decode(@NotNull CustomTypeValue value) {
        return value.value.toString();
      }

      @NotNull @Override public CustomTypeValue encode(@NotNull Object value) {
        throw new UnsupportedOperationException();
      }
    });
    return new SimpleResponseReader(recordSet, EMPTY_OPERATION.variables(), new ScalarTypeAdapters(customTypeAdapters));
  }

  private static ScalarType scalarTypeFor(final Class clazz) {
    return new ScalarType() {
      @Override public String typeName() {
        return clazz.getName();
      }

      @Override public String className() {
        return clazz.getName();
      }
    };
  }

  private static final ScalarType OBJECT_CUSTOM_TYPE = new ScalarType() {
    @Override public String typeName() {
      return String.class.getName();
    }

    @Override public String className() {
      return String.class.getName();
    }
  };

  private static final ScalarType DATE_CUSTOM_TYPE = new ScalarType() {
    @Override public String typeName() {
      return Date.class.getName();
    }

    @Override public String className() {
      return Date.class.getName();
    }
  };

  private static final ScalarType URL_CUSTOM_TYPE = new ScalarType() {
    @Override public String typeName() {
      return URL.class.getName();
    }

    @Override public String className() {
      return URL.class.getName();
    }
  };

  private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyyy-mm-dd");

  private static final Operation EMPTY_OPERATION = new Operation() {
    @Override public String queryDocument() {
      throw new UnsupportedOperationException();
    }

    @Override public Variables variables() {
      return EMPTY_VARIABLES;
    }

    @Override public ResponseFieldMapper responseFieldMapper() {
      throw new UnsupportedOperationException();
    }

    @Override public Object wrapData(Data data) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public OperationName name() {
      return new OperationName() {
        @Override public String name() {
          return "test";
        }
      };
    }

    @NotNull @Override public String operationId() {
      return "";
    }

    @NotNull @Override public Response parse(@NotNull BufferedSource source) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public Response parse(@NotNull BufferedSource source, @NotNull ScalarTypeAdapters scalarTypeAdapters) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public ByteString composeRequestBody() {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public ByteString composeRequestBody(@NotNull ScalarTypeAdapters scalarTypeAdapters) {
      throw new UnsupportedOperationException();
    }
  };
}
