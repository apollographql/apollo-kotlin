package com.apollographql.android.api.graphql;

import java.io.IOException;

/** TODO **/
public interface ResponseReader {
  ResponseReader toBufferedReader() throws IOException;

  void read(ValueHandler handler, Field... fields) throws IOException;

  interface ValueHandler {
    void handle(int fieldIndex, Object value) throws IOException;
  }
}
