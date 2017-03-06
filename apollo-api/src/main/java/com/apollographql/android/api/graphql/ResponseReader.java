package com.apollographql.android.api.graphql;

import java.io.IOException;

/** TODO **/
public interface ResponseReader {
  <T> T read(Field field) throws IOException;

  interface ValueHandler {
    void handle(int fieldIndex, Object value) throws IOException;
  }
}
