package com.apollographql.api.graphql;

import java.io.IOException;

/** TODO java doc **/
public interface ResponseReader {
  ResponseReader toBufferedReader() throws IOException;

  void read(ValueHandler handler, Field... fields) throws IOException;

  interface ValueHandler {
    void handle(int fieldIndex, Object value) throws IOException;
  }
}
