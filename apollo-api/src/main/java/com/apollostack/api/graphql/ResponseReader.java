package com.apollostack.api.graphql;

import java.io.IOException;

/** TODO java doc **/
public interface ResponseReader {
  ResponseReader buffer() throws IOException;

  void read(ValueHandler handler, Field... fields) throws IOException;

  interface ValueHandler {
    void handle(int fieldIndex, Object value) throws IOException;
  }
}
