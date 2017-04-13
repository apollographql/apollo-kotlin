package com.apollographql.apollo;

import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;

import java.io.IOException;

public final class TestUtils {
  public static final Query EMPTY_QUERY = new Query() {
    @Override public String queryDocument() {
      return "";
    }

    @Override public Variables variables() {
      return EMPTY_VARIABLES;
    }

    @Override public ResponseFieldMapper<Data> responseFieldMapper() {
      return new ResponseFieldMapper<Data>() {
        @Override public Data map(ResponseReader responseReader) throws IOException {
          return null;
        }
      };
    }

    @Override public Object wrapData(Data data) {
      return data;
    }
  };

  private TestUtils() {
  }
}
