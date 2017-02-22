package com.apollographql.android.api.graphql;

import java.io.IOException;

/** TODO **/
public interface ResponseReader {
  void read(ValueHandler handler, Field... fields) throws IOException;

  <T> T read(Field field) throws IOException;

  Operation operation();

  interface ValueHandler {
    void handle(int fieldIndex, Object value) throws IOException;
  }
}
