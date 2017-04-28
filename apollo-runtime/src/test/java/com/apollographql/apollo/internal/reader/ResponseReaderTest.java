package com.apollographql.apollo.internal.reader;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.field.MapFieldValueResolver;

import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.fail;

public class ResponseReaderTest {

  @Test public void readString() throws Exception {
    Field successField = Field.forString("successFieldResponseName", "successFieldName", null, false);
    Field classCastExceptionField = Field.forString("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", "response1");
    recordSet.put("successFieldName", "response2");
    recordSet.put("classCastExceptionFieldResponseName", 1);
    checkReadFields(recordSet, successField, classCastExceptionField, "response1");
  }

  @Test public void readInt() throws Exception {
    Field successField = Field.forInt("successFieldResponseName", "successFieldName", null, false);
    Field classCastExceptionField = Field.forInt("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", BigDecimal.valueOf(1));
    recordSet.put("successFieldName", BigDecimal.valueOf(2));
    recordSet.put("classCastExceptionFieldResponseName", "anything");
    checkReadFields(recordSet, successField, classCastExceptionField, 1);
  }

  @Test public void readLong() throws Exception {
    Field successField = Field.forLong("successFieldResponseName", "successFieldName", null, false);
    Field classCastExceptionField = Field.forLong("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", BigDecimal.valueOf(1));
    recordSet.put("successFieldName", BigDecimal.valueOf(2));
    recordSet.put("classCastExceptionFieldResponseName", "anything");
    checkReadFields(recordSet, successField, classCastExceptionField, 1);
  }

  @Test public void readDouble() throws Exception {
    Field successField = Field.forDouble("successFieldResponseName", "successFieldName", null, false);
    Field classCastExceptionField = Field.forDouble("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", BigDecimal.valueOf(1.1));
    recordSet.put("successFieldName", BigDecimal.valueOf(2.2));
    recordSet.put("classCastExceptionFieldResponseName", "anything");
    checkReadFields(recordSet, successField, classCastExceptionField, 1.1D);
  }

  @Test public void readBoolean() throws Exception {
    Field successField = Field.forBoolean("successFieldResponseName", "successFieldName", null, false);
    Field classCastExceptionField = Field.forBoolean("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", true);
    recordSet.put("successFieldName", false);
    recordSet.put("classCastExceptionFieldResponseName", "anything");
    checkReadFields(recordSet, successField, classCastExceptionField, true);
  }

  @Test public void readObject() throws Exception {
    final Object responseObject1 = new Object();
    final Object responseObject2 = new Object();
    final Field.ObjectReader objectReader = new Field.ObjectReader<Object>() {
      @Override public Object read(ResponseReader reader) throws IOException {
        return responseObject1;
      }
    };
    Field successField = Field.forObject("successFieldResponseName", "successFieldName", null, false, objectReader);
    Field classCastExceptionField = Field.forObject("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false, new Field.ObjectReader<Object>() {
          @Override public Object read(ResponseReader reader) throws IOException {
            return reader.read(Field.forString("anything", "anything", null, true));
          }
        });

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", responseObject1);
    recordSet.put("successFieldName", responseObject2);
    recordSet.put("classCastExceptionFieldResponseName", "anything");
    checkReadFields(recordSet, successField, classCastExceptionField, responseObject1);
  }

  @Test public void readScalarList() throws Exception {
    final Field.ListReader listReader = new Field.ListReader<String>() {
      @Override public String read(Field.ListItemReader reader) throws IOException {
        return reader.readString();
      }
    };

    Field successField = Field.forList("successFieldResponseName", "successFieldName", null, false,
        listReader);
    Field classCastExceptionField = Field.forList("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false, listReader);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList("value1", "value2", "value3"));
    recordSet.put("successFieldName", asList("value4", "value5"));
    recordSet.put("classCastExceptionFieldResponseName", "anything");
    checkReadFields(recordSet, successField, classCastExceptionField, asList("value1", "value2", "value3"));
  }

  @Test public void readObjectList() throws Exception {
    final Object responseObject1 = new Object();
    final Object responseObject2 = new Object();

    final Field.ObjectReader objectReader = new Field.ObjectReader() {
      @Override public Object read(ResponseReader reader) throws IOException {
        return responseObject1;
      }
    };

    Field successField = Field.forList("successFieldResponseName", "successFieldName", null, false, objectReader);
    Field classCastExceptionField = Field.forList("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false, objectReader);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList(responseObject1));
    recordSet.put("successFieldName", asList(responseObject2));
    recordSet.put("classCastExceptionFieldResponseName", "anything");
    checkReadFields(recordSet, successField, classCastExceptionField, asList(responseObject1));
  }

  @Test public void readCustom() throws Exception {
    Field successField = Field.forCustomType("successFieldResponseName", "successFieldName", null, false, CUSTOM_TYPE);
    Field classCastExceptionField = Field.forCustomType("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false, CUSTOM_TYPE);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", "2017-04-16");
    recordSet.put("successFieldName", "2018-04-16");
    recordSet.put("classCastExceptionFieldResponseName", "anything");
    checkReadFields(recordSet, successField, classCastExceptionField, DATE_TIME_FORMAT.parse("2017-04-16")
    );
  }

  @Test public void readConditional() throws Exception {
    final Object responseObject1 = new Object();
    final Object responseObject2 = new Object();

    Field.ConditionalTypeReader conditionalTypeReader = new Field.ConditionalTypeReader() {
      @Override public Object read(String conditionalType, ResponseReader reader) throws IOException {
        if (conditionalType.equals("responseObject1")) {
          return responseObject1;
        } else {
          return responseObject2;
        }
      }
    };

    Field successField = Field.forConditionalType("successFieldResponseName", "successFieldName", conditionalTypeReader);
    Field classCastExceptionField = Field.forConditionalType("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        conditionalTypeReader);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", "responseObject1");
    recordSet.put("successFieldName", "responseObject2");
    recordSet.put("classCastExceptionFieldResponseName", 1);
    checkReadFields(recordSet, successField, classCastExceptionField, responseObject1);
  }

  @Test public void optionalFields() throws Exception {
    List<Field> fields = asList(
        Field.forString("stringField", "stringField", null, true),
        Field.forInt("intField", "intField", null, true),
        Field.forLong("longField", "longField", null, true),
        Field.forDouble("doubleField", "doubleField", null, true),
        Field.forBoolean("booleanField", "booleanField", null, true),
        Field.forObject("objectField", "objectField", null, true, new Field.ObjectReader<Object>() {
          @Override public Object read(ResponseReader reader) throws IOException {
            return new Object();
          }
        }),
        Field.forList("scalarListField", "scalarListField", null, true, new Field.ListReader<String>() {
          @Override public String read(Field.ListItemReader reader) throws IOException {
            return reader.readString();
          }
        }),
        Field.forList("objectListField", "objectListField", null, true,
            new Field.ObjectReader<Object>() {
              @Override public Object read(ResponseReader reader) throws IOException {
                return new Object();
              }
            }),
        Field.forCustomType("customTypeField", "customTypeField", null, true,
            CUSTOM_TYPE)
    );

    RealResponseReader<Map<String, Object>> responseReader = responseReader(Collections.<String, Object>emptyMap());
    for (Field field : fields) {
      assertThat(responseReader.read(field)).isNull();
    }
  }

  @Test public void mandatoryFieldsNpe() throws Exception {
    List<Field> fields = asList(
        Field.forString("stringField", "stringField", null, false),
        Field.forInt("intField", "intField", null, false),
        Field.forLong("longField", "longField", null, false),
        Field.forDouble("doubleField", "doubleField", null, false),
        Field.forBoolean("booleanField", "booleanField", null, false),
        Field.forObject("objectField", "objectField", null, false, new Field.ObjectReader<Object>() {
          @Override public Object read(ResponseReader reader) throws IOException {
            return new Object();
          }
        }),
        Field.forList("scalarListField", "scalarListField", null, false, new Field.ListReader<String>() {
          @Override public String read(Field.ListItemReader reader) throws IOException {
            return reader.readString();
          }
        }),
        Field.forList("objectListField", "objectListField", null, false,
            new Field.ObjectReader<Object>() {
              @Override public Object read(ResponseReader reader) throws IOException {
                return new Object();
              }
            }),
        Field.forCustomType("customTypeField", "customTypeField", null, false,
            CUSTOM_TYPE)
    );

    RealResponseReader<Map<String, Object>> responseReader = responseReader(Collections.<String, Object>emptyMap());
    for (Field field : fields) {
      try {
        responseReader.read(field);
        fail("expected NullPointerException for field: " + field);
      } catch (NullPointerException expected) {
        //expected
      }
    }
  }

  private void checkReadFields(Map<String, Object> recordSet, Field successField, Field classCastExceptionField,
      Object expectedSuccessValue) throws IOException {
    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.read(successField)).isEqualTo(expectedSuccessValue);
    try {
      responseReader.read(classCastExceptionField);
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @SuppressWarnings("unchecked") private static RealResponseReader<Map<String, Object>> responseReader(
      Map<String, Object> recordSet) {
    Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new HashMap<>();
    customTypeAdapters.put(CUSTOM_TYPE, new CustomTypeAdapter() {
      @Override public Object decode(String value) {
        try {
          return DATE_TIME_FORMAT.parse(value);
        } catch (ParseException e) {
          throw new ClassCastException();
        }
      }

      @Override public String encode(Object value) {
        return null;
      }
    });
    return new RealResponseReader<>(EMPTY_OPERATION, recordSet, new MapFieldValueResolver(), customTypeAdapters,
        NO_OP_NORMALIZER);
  }

  private static final ScalarType CUSTOM_TYPE = new ScalarType() {
    @Override public String typeName() {
      return Date.class.getName();
    }

    @Override public Class javaType() {
      return Date.class;
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
  };

  @SuppressWarnings("unchecked") private static final ResponseNormalizer NO_OP_NORMALIZER =
      new ResponseNormalizer(CacheKeyResolver.DEFAULT) {
        @Override public void willResolveRootQuery(Operation operation) {
        }

        @Override public void willResolve(Field field, Operation.Variables variables) {
        }

        @Override public void didResolve(Field field, Operation.Variables variables) {
        }

        @Override public void didParseScalar(Object value) {
        }

        @Override public void willParseObject(Optional objectMap) {
        }

        @Override public void didParseObject(Optional objectMap) {
        }

        @Override public void didParseList(List array) {
        }

        @Override public void willParseElement(int atIndex) {
        }

        @Override public void didParseElement(int atIndex) {
        }

        @Override public void didParseNull() {
        }

        @Override public Collection<Record> records() {
          return Collections.emptyList();
        }

        @Override public Set<String> dependentKeys() {
          return Collections.emptySet();
        }
      };
}
