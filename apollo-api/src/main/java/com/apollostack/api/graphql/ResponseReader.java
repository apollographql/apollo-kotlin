package com.apollostack.api.graphql;

import java.io.IOException;

/** TODO java doc **/
public interface ResponseReader {
  ResponseReader toBufferedReader() throws IOException;

  void read(ValueHandler handler, Field... fields) throws IOException;

  interface ValueHandler {
    void handle(int fieldIndex, Object value) throws IOException;
  }

  interface ListItemReader {

    String readString() throws IOException;

    Integer readInt() throws IOException;

    Long readLong() throws IOException;

    Double readDouble() throws IOException;

    Boolean readBoolean() throws IOException;

    <T> T readObject(Field.NestedReader<T> nestedReader) throws IOException;
  }
}
