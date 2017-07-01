package com.apollographql.apollo.internal.reader;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.CacheKey;
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

import javax.annotation.Nonnull;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.fail;

public class ResponseReaderTest {

  @Test public void readString() throws Exception {
    ResponseField successField = ResponseField.forString("successFieldResponseName", "successFieldName", null, false);
    ResponseField classCastExceptionField = ResponseField.forString("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", "response1");
    recordSet.put("successFieldName", "response2");
    recordSet.put("classCastExceptionFieldResponseName", 1);

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readString(successField)).isEqualTo("response1");
    try {
      responseReader.readString(classCastExceptionField);
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readInt() throws Exception {
    ResponseField successField = ResponseField.forInt("successFieldResponseName", "successFieldName", null, false);
    ResponseField classCastExceptionField = ResponseField.forInt("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", BigDecimal.valueOf(1));
    recordSet.put("successFieldName", BigDecimal.valueOf(2));
    recordSet.put("classCastExceptionFieldResponseName", "anything");

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readInt(successField)).isEqualTo(1);
    try {
      responseReader.readInt(classCastExceptionField);
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readLong() throws Exception {
    ResponseField successField = ResponseField.forLong("successFieldResponseName", "successFieldName", null, false);
    ResponseField classCastExceptionField = ResponseField.forLong("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", BigDecimal.valueOf(1));
    recordSet.put("successFieldName", BigDecimal.valueOf(2));
    recordSet.put("classCastExceptionFieldResponseName", "anything");

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readLong(successField)).isEqualTo(1);
    try {
      responseReader.readLong(classCastExceptionField);
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readDouble() throws Exception {
    ResponseField successField = ResponseField.forDouble("successFieldResponseName", "successFieldName", null, false);
    ResponseField classCastExceptionField = ResponseField.forDouble("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", BigDecimal.valueOf(1.1));
    recordSet.put("successFieldName", BigDecimal.valueOf(2.2));
    recordSet.put("classCastExceptionFieldResponseName", "anything");

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readDouble(successField)).isEqualTo(1.1D);
    try {
      responseReader.readDouble(classCastExceptionField);
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readBoolean() throws Exception {
    ResponseField successField = ResponseField.forBoolean("successFieldResponseName", "successFieldName", null, false);
    ResponseField classCastExceptionField = ResponseField.forBoolean("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", true);
    recordSet.put("successFieldName", false);
    recordSet.put("classCastExceptionFieldResponseName", "anything");

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readBoolean(successField)).isTrue();
    try {
      responseReader.readBoolean(classCastExceptionField);
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readObject() throws Exception {
    final Object responseObject1 = new Object();
    final Object responseObject2 = new Object();
    ResponseField successField = ResponseField.forObject("successFieldResponseName", "successFieldName", null, false);
    ResponseField classCastExceptionField = ResponseField.forObject("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", responseObject1);
    recordSet.put("successFieldName", responseObject2);
    recordSet.put("classCastExceptionFieldResponseName", "anything");

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readObject(successField, new ResponseReader.ObjectReader<Object>() {
      @Override public Object read(ResponseReader reader) throws IOException {
        return responseObject1;
      }
    })).isEqualTo(responseObject1);

    try {
      responseReader.readObject(classCastExceptionField, new ResponseReader.ObjectReader<Object>() {
        @Override public Object read(ResponseReader reader) throws IOException {
          return reader.readString(ResponseField.forString("anything", "anything", null, true));
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readCustom() throws Exception {
    ResponseField successField = ResponseField.forCustomType("successFieldResponseName", "successFieldName", null, false, CUSTOM_TYPE);
    ResponseField classCastExceptionField = ResponseField.forCustomType("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false, CUSTOM_TYPE);

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", "2017-04-16");
    recordSet.put("successFieldName", "2018-04-16");
    recordSet.put("classCastExceptionFieldResponseName", 0);

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readCustomType((ResponseField.CustomTypeField) successField)).isEqualTo(DATE_TIME_FORMAT.parse("2017-04-16"));
    try {
      responseReader.readCustomType((ResponseField.CustomTypeField) classCastExceptionField);
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readConditional() throws Exception {
    final Object responseObject1 = new Object();
    final Object responseObject2 = new Object();

    ResponseField successField = ResponseField.forFragment("successFieldResponseName", "successFieldName", Collections.<String>emptyList());
    ResponseField classCastExceptionField = ResponseField.forFragment("classCastExceptionFieldResponseName",
        "classCastExceptionFieldName", Collections.<String>emptyList());

    Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", "responseObject1");
    recordSet.put("successFieldName", "responseObject2");
    recordSet.put("classCastExceptionFieldResponseName", 1);

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readConditional((ResponseField.ConditionalTypeField) successField, new ResponseReader.ConditionalTypeReader<Object>() {
      @Override public Object read(String conditionalType, ResponseReader reader) throws IOException {
        if (conditionalType.equals("responseObject1")) {
          return responseObject1;
        } else {
          return responseObject2;
        }
      }
    })).isEqualTo(responseObject1);

    try {
      responseReader.readConditional((ResponseField.ConditionalTypeField) classCastExceptionField, new ResponseReader.ConditionalTypeReader<Object>() {
        @Override public Object read(String conditionalType, ResponseReader reader) throws IOException {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readStringList() throws Exception {
    ResponseField successField = ResponseField.forScalarList("successFieldResponseName", "successFieldName", null, false);
    ResponseField classCastExceptionField = ResponseField.forScalarList("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList("value1", "value2", "value3"));
    recordSet.put("successFieldName", asList("value4", "value5"));
    recordSet.put("classCastExceptionFieldResponseName", "anything");

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
        return reader.readString();
      }
    })).isEqualTo(asList("value1", "value2", "value3"));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readIntList() throws Exception {
    ResponseField successField = ResponseField.forScalarList("successFieldResponseName", "successFieldName", null, false);
    ResponseField classCastExceptionField = ResponseField.forScalarList("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
    recordSet.put("successFieldName", asList(BigDecimal.valueOf(4), BigDecimal.valueOf(5)));
    recordSet.put("classCastExceptionFieldResponseName", "anything");

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
        return reader.readInt();
      }
    })).isEqualTo(asList(1, 2, 3));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readLongList() throws Exception {
    ResponseField successField = ResponseField.forScalarList("successFieldResponseName", "successFieldName", null, false);
    ResponseField classCastExceptionField = ResponseField.forScalarList("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
    recordSet.put("successFieldName", asList(BigDecimal.valueOf(4), BigDecimal.valueOf(5)));
    recordSet.put("classCastExceptionFieldResponseName", "anything");

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
        return reader.readInt();
      }
    })).isEqualTo(asList(1, 2, 3));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readDoubleList() throws Exception {
    ResponseField successField = ResponseField.forScalarList("successFieldResponseName", "successFieldName", null, false);
    ResponseField classCastExceptionField = ResponseField.forScalarList("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
    recordSet.put("successFieldName", asList(BigDecimal.valueOf(4), BigDecimal.valueOf(5)));
    recordSet.put("classCastExceptionFieldResponseName", "anything");

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
        return reader.readDouble();
      }
    })).isEqualTo(asList(1D, 2D, 3D));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readBooleanList() throws Exception {
    ResponseField successField = ResponseField.forScalarList("successFieldResponseName", "successFieldName", null, false);
    ResponseField classCastExceptionField = ResponseField.forScalarList("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList(true, false, true));
    recordSet.put("successFieldName", asList(false, false));
    recordSet.put("classCastExceptionFieldResponseName", "anything");

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
        return reader.readBoolean();
      }
    })).isEqualTo(asList(true, false, true));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readCustomList() throws Exception {
    ResponseField successField = ResponseField.forScalarList("successFieldResponseName", "successFieldName", null, false);
    ResponseField classCastExceptionField = ResponseField.forScalarList("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList("2017-04-16", "2017-04-17", "2017-04-18"));
    recordSet.put("successFieldName", asList("2017-04-19", "2017-04-20"));
    recordSet.put("classCastExceptionFieldResponseName", "anything");

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
        return reader.readCustomType(CUSTOM_TYPE);
      }
    })).isEqualTo(asList(DATE_TIME_FORMAT.parse("2017-04-16"), DATE_TIME_FORMAT.parse("2017-04-17"), DATE_TIME_FORMAT.parse("2017-04-18")));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void readObjectList() throws Exception {
    ResponseField successField = ResponseField.forScalarList("successFieldResponseName", "successFieldName", null, false);
    ResponseField classCastExceptionField = ResponseField.forScalarList("classCastExceptionFieldResponseName", "classCastExceptionFieldName",
        null, false);

    final Object responseObject1 = new Object();
    final Object responseObject2 = new Object();
    final Object responseObject3 = new Object();
    final Object responseObject4 = new Object();
    final Object responseObject5 = new Object();
    final Object responseObject6 = new Object();

    final Map<String, Object> recordSet = new HashMap<>();
    recordSet.put("successFieldResponseName", asList(responseObject1, responseObject2, responseObject3));
    recordSet.put("successFieldName", asList(responseObject4, responseObject5));
    recordSet.put("classCastExceptionFieldResponseName", "anything");

    RealResponseReader<Map<String, Object>> responseReader = responseReader(recordSet);
    assertThat(responseReader.readList(successField, new ResponseReader.ListReader() {
      int index = 0;

      @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
        return reader.readObject(new ResponseReader.ObjectReader<Object>() {
          @Override public Object read(ResponseReader reader) throws IOException {
            return ((List) recordSet.get("successFieldResponseName")).get(index++);
          }
        });
      }
    })).isEqualTo(asList(responseObject1, responseObject2, responseObject3));

    try {
      responseReader.readList(classCastExceptionField, new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
          return null;
        }
      });
      fail("expected ClassCastException");
    } catch (ClassCastException expected) {
      // expected
    }
  }

  @Test public void optionalFieldsIOException() throws Exception {
    RealResponseReader<Map<String, Object>> responseReader = responseReader(Collections.<String, Object>emptyMap());
    responseReader.readString(ResponseField.forString("stringField", "stringField", null, true));
    responseReader.readInt(ResponseField.forInt("intField", "intField", null, true));
    responseReader.readLong(ResponseField.forLong("longField", "longField", null, true));
    responseReader.readDouble(ResponseField.forDouble("doubleField", "doubleField", null, true));
    responseReader.readBoolean(ResponseField.forBoolean("booleanField", "booleanField", null, true));
    responseReader.readObject(ResponseField.forObject("objectField", "objectField", null, true), new ResponseReader.ObjectReader<Object>() {
      @Override public Object read(ResponseReader reader) throws IOException {
        return null;
      }
    });
    responseReader.readList(ResponseField.forScalarList("scalarListField", "scalarListField", null, true), new ResponseReader.ListReader() {
      @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
        return null;
      }
    });
    responseReader.readCustomType((ResponseField.CustomTypeField) ResponseField.forCustomType("customTypeField", "customTypeField",
        null, true, CUSTOM_TYPE));
  }

  @Test public void mandatoryFieldsIOException() throws Exception {
    final RealResponseReader<Map<String, Object>> responseReader = responseReader(Collections.<String, Object>emptyMap());

    try {
      responseReader.readString(ResponseField.forString("stringField", "stringField", null, false));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readInt(ResponseField.forInt("intField", "intField", null, false));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readLong(ResponseField.forLong("longField", "longField", null, false));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readDouble(ResponseField.forDouble("doubleField", "doubleField", null, false));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readBoolean(ResponseField.forBoolean("booleanField", "booleanField", null, false));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readObject(ResponseField.forObject("objectField", "objectField", null, false), new ResponseReader.ObjectReader<Object>() {
        @Override public Object read(ResponseReader reader) throws IOException {
          return null;
        }
      });
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readList(ResponseField.forScalarList("scalarListField", "scalarListField", null, false), new ResponseReader.ListReader() {
        @Override public Object read(ResponseReader.ListItemReader reader) throws IOException {
          return null;
        }
      });
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readCustomType((ResponseField.CustomTypeField) ResponseField.forCustomType("customTypeField", "customTypeField", null, false, CUSTOM_TYPE));
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }

    try {
      responseReader.readConditional((ResponseField.ConditionalTypeField) ResponseField.forFragment("__typename", "__typename", Collections.<String>emptyList()), new ResponseReader.ConditionalTypeReader<Object>() {
        @Override public Object read(String conditionalType, ResponseReader reader) throws IOException {
          return null;
        }
      });
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
    }
  }

  private void checkNPE(Runnable action) {
    try {

      fail("expected NullPointerException");
    } catch (NullPointerException expected) {
      //expected
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
    return new RealResponseReader<>(EMPTY_OPERATION.variables(), recordSet, new MapFieldValueResolver(),
        customTypeAdapters, NO_OP_NORMALIZER);
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

    @Nonnull @Override public OperationName name() {
      return null;
    }
  };

  @SuppressWarnings("unchecked") private static final ResponseNormalizer NO_OP_NORMALIZER = new ResponseNormalizer() {
    @Override public void willResolveRootQuery(Operation operation) {
    }

    @Override public void willResolve(ResponseField field, Operation.Variables variables) {
    }

    @Override public void didResolve(ResponseField field, Operation.Variables variables) {
    }

    @Override public void didResolveScalar(Object value) {
    }

    @Override public void willResolveObject(ResponseField field, Optional objectSource) {
    }

    @Override public void didResolveObject(ResponseField Field, Optional objectSource) {
    }

    @Nonnull @Override public CacheKey resolveCacheKey(@Nonnull ResponseField field, @Nonnull Object record) {
      return CacheKey.NO_KEY;
    }

    @Override public void didResolveList(List array) {
    }

    @Override public void willResolveElement(int atIndex) {
    }

    @Override public void didResolveElement(int atIndex) {
    }

    @Override public void didResolveNull() {
    }

    @Override public Collection<Record> records() {
      return Collections.emptyList();
    }

    @Override public Set<String> dependentKeys() {
      return Collections.emptySet();
    }
  };
}
