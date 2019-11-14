package com.apollographql.apollo.api.internal;

import com.apollographql.apollo.api.CustomTypeAdapter;
import com.apollographql.apollo.api.CustomTypeValue;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.ScalarTypeAdapters;
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
  private final List<ResponseField.Condition> NO_CONDITIONS = Collections.emptyList();

  @Test public void readString() {
    final ResponseField successField = ResponseField.forString("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forString("classCastExceptionField", "classCastExceptionField", null, false,
        NO_CONDITIONS);

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
    final ResponseField successField = ResponseField.forInt("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forInt("classCastExceptionField", "classCastExceptionField", null, false,
        NO_CONDITIONS);

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
    final ResponseField successField = ResponseField.forLong("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forLong("classCastExceptionField", "classCastExceptionField", null, false,
        NO_CONDITIONS);

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
    final ResponseField successField = ResponseField.forDouble("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forDouble("classCastExceptionField", "classCastExceptionField", null, false,
        NO_CONDITIONS);

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
    final ResponseField successField = ResponseField.forBoolean("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forBoolean("classCastExceptionField", "classCastExceptionField", null,
        false, NO_CONDITIONS);

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
    final ResponseField successField = ResponseField.forObject("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forObject("classCastExceptionField", "classCastExceptionField", null, false,
        NO_CONDITIONS);

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
          return reader.readString(ResponseField.forString("anything", "anything", null, true, NO_CONDITIONS));
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readCustom() throws ParseException {
    final ResponseField.CustomTypeField successField = ResponseField.forCustomType("successFieldResponseName", "successFieldName", null,
        false, DATE_CUSTOM_TYPE, NO_CONDITIONS);
    final ResponseField.CustomTypeField classCastExceptionField = ResponseField.forCustomType("classCastExceptionField",
        "classCastExceptionField", null, false, DATE_CUSTOM_TYPE, NO_CONDITIONS);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", "2017-04-16");
    recordSet.put("successFieldName", "2018-04-16");
    recordSet.put("classCastExceptionField", 0);

    final SimpleResponseReader responseReader = responseReader(recordSet);

    assertThat(responseReader.readCustomType(successField)).isEqualTo(DATE_TIME_FORMAT.parse("2017-04-16"));
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

      @Override public Class javaType() {
        return Map.class;
      }
    };

    final ResponseField.CustomTypeField successField = ResponseField.forCustomType("successFieldResponseName", "successFieldName", null,
        false, mapScalarType, NO_CONDITIONS);

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

      @Override public Class javaType() {
        return List.class;
      }
    };
    final ResponseField.CustomTypeField successField = ResponseField.forCustomType("successFieldResponseName", "successFieldName", null,
        false, listScalarType, NO_CONDITIONS);

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
        scalarTypeFor(String.class), NO_CONDITIONS);
    final ResponseField.CustomTypeField booleanField = ResponseField.forCustomType("booleanField", "booleanField", null, false,
        scalarTypeFor(Boolean.class), NO_CONDITIONS);
    final ResponseField.CustomTypeField integerField = ResponseField.forCustomType("integerField", "integerField", null, false,
        scalarTypeFor(Integer.class), NO_CONDITIONS);
    final ResponseField.CustomTypeField longField = ResponseField.forCustomType("longField", "longField", null, false,
        scalarTypeFor(Long.class), NO_CONDITIONS);
    final ResponseField.CustomTypeField floatField = ResponseField.forCustomType("floatField", "floatField", null, false,
        scalarTypeFor(Float.class), NO_CONDITIONS);
    final ResponseField.CustomTypeField doubleField = ResponseField.forCustomType("doubleField", "doubleField", null, false,
        scalarTypeFor(Double.class), NO_CONDITIONS);
    final ResponseField.CustomTypeField unsupportedField = ResponseField.forCustomType("unsupportedField", "unsupportedField", null, false,
        scalarTypeFor(RuntimeException.class), NO_CONDITIONS);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put(stringField.responseName(), "string");
    recordSet.put(booleanField.responseName(), true);
    recordSet.put(integerField.responseName(), BigDecimal.valueOf(1));
    recordSet.put(longField.responseName(), BigDecimal.valueOf(2));
    recordSet.put(floatField.responseName(), BigDecimal.valueOf(3.99));
    recordSet.put(doubleField.responseName(), BigDecimal.valueOf(4.99));
    recordSet.put(unsupportedField.responseName(), "smth");

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat(responseReader.readCustomType(stringField)).isEqualTo("string");
    assertThat(responseReader.readCustomType(booleanField)).isEqualTo(true);
    assertThat(responseReader.readCustomType(integerField)).isEqualTo(1);
    assertThat(responseReader.readCustomType(longField)).isEqualTo(2);
    assertThat(responseReader.readCustomType(floatField)).isEqualTo(3.99f);
    assertThat(responseReader.readCustomType(doubleField)).isEqualTo(4.99d);

    try {
      responseReader.readCustomType(unsupportedField);
      fail("Expect IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  @Test public void readConditional() {
    final Object responseObject1 = new Object();
    final Object responseObject2 = new Object();

    final ResponseField successField = ResponseField.forFragment("successFieldResponseName", "successFieldName",
        Collections.<String>emptyList());
    final ResponseField classCastExceptionField = ResponseField.forFragment("classCastExceptionField", "classCastExceptionField",
        Collections.<String>emptyList());

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", "responseObject1");
    recordSet.put("successFieldName", "responseObject2");
    recordSet.put("classCastExceptionField", 1);

    final SimpleResponseReader responseReader = responseReader(recordSet);

    assertThat(responseReader.readConditional(successField, new ResponseReader.ConditionalTypeReader<Object>() {
      @Override public Object read(String conditionalType, ResponseReader reader) {
        if (conditionalType.equals("responseObject1")) {
          return responseObject1;
        } else {
          return responseObject2;
        }
      }
    })).isEqualTo(responseObject1);

    try {
      responseReader.readConditional(classCastExceptionField, new ResponseReader.ConditionalTypeReader<Object>() {
        @Override public Object read(String conditionalType, ResponseReader reader) {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readStringList() {
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        NO_CONDITIONS);

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
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        NO_CONDITIONS);

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
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        NO_CONDITIONS);

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
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        NO_CONDITIONS);

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
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        NO_CONDITIONS);

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
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        NO_CONDITIONS);

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
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        NO_CONDITIONS);

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
    final ResponseField successField = ResponseField.forList("successFieldResponseName", "successFieldName", null, false, NO_CONDITIONS);
    final ResponseField classCastExceptionField = ResponseField.forList("classCastExceptionField", "classCastExceptionField", null, false,
        NO_CONDITIONS);

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
    responseReader.readString(ResponseField.forString("stringField", "stringField", null, true, NO_CONDITIONS));
    responseReader.readInt(ResponseField.forInt("intField", "intField", null, true, NO_CONDITIONS));
    responseReader.readLong(ResponseField.forLong("longField", "longField", null, true, NO_CONDITIONS));
    responseReader.readDouble(ResponseField.forDouble("doubleField", "doubleField", null, true, NO_CONDITIONS));
    responseReader.readBoolean(ResponseField.forBoolean("booleanField", "booleanField", null, true, NO_CONDITIONS));
    responseReader.readObject(ResponseField.forObject("objectField", "objectField", null, true, NO_CONDITIONS),
        new ResponseReader.ObjectReader<Object>() {
          @Override public Object read(ResponseReader reader) {
            return null;
          }
        }
    );
    responseReader.readList(ResponseField.forList("scalarListField", "scalarListField", null, true, NO_CONDITIONS),
        new ResponseReader.ListReader() {
          @Override public Object read(ResponseReader.ListItemReader reader) {
            return null;
          }
        }
    );
    responseReader.readCustomType(ResponseField.forCustomType("customTypeField", "customTypeField", null, true, DATE_CUSTOM_TYPE,
        NO_CONDITIONS));
  }

  @Test public void mandatoryFieldsIOException() {
    final SimpleResponseReader responseReader = responseReader(Collections.<String, Object>emptyMap());

    try {
      responseReader.readString(ResponseField.forString("stringField", "stringField", null, false, NO_CONDITIONS));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readInt(ResponseField.forInt("intField", "intField", null, false, NO_CONDITIONS));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readLong(ResponseField.forLong("longField", "longField", null, false, NO_CONDITIONS));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readDouble(ResponseField.forDouble("doubleField", "doubleField", null, false, NO_CONDITIONS));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readBoolean(ResponseField.forBoolean("booleanField", "booleanField", null, false, NO_CONDITIONS));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readObject(ResponseField.forObject("objectField", "objectField", null, false, NO_CONDITIONS),
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
      responseReader.readList(ResponseField.forList("scalarListField", "scalarListField", null, false, NO_CONDITIONS),
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
          NO_CONDITIONS));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readConditional(ResponseField.forFragment("__typename", "__typename", Collections.<String>emptyList()),
          new ResponseReader.ConditionalTypeReader<Object>() {
            @Override public Object read(String conditionalType, ResponseReader reader) {
              return null;
            }
          }
      );
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }
  }

  @Test public void readScalarListWithNulls() {
    final ResponseField scalarList = ResponseField.forList("list", "list", null, false, NO_CONDITIONS);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("list", asList(null, "item2", "item3", null, "item5", null));

    final SimpleResponseReader responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(scalarList, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) {
        return reader.readString();
      }
    })).isEqualTo(asList(null, "item2", "item3", null, "item5", null));
  }

  @Test public void read_object_list_with_nulls() {
    final ResponseField listField = ResponseField.forList("list", "list", null, false, NO_CONDITIONS);
    final ResponseField indexField = ResponseField.forList("index", "index", null, false, NO_CONDITIONS);
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
    Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new HashMap<>();
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

      @Override public Class javaType() {
        return clazz;
      }
    };
  }

  private static final ScalarType OBJECT_CUSTOM_TYPE = new ScalarType() {
    @Override public String typeName() {
      return String.class.getName();
    }

    @Override public Class javaType() {
      return String.class;
    }
  };

  private static final ScalarType DATE_CUSTOM_TYPE = new ScalarType() {
    @Override public String typeName() {
      return Date.class.getName();
    }

    @Override public Class javaType() {
      return Date.class;
    }
  };

  private static final ScalarType URL_CUSTOM_TYPE = new ScalarType() {
    @Override public String typeName() {
      return URL.class.getName();
    }

    @Override public Class javaType() {
      return URL.class;
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

    @NotNull @Override public Response parse(@NotNull Map response, @NotNull ScalarTypeAdapters scalarTypeAdapters) {
      throw new UnsupportedOperationException();
    }
  };
}
