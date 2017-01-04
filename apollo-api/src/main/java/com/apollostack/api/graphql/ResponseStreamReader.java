package com.apollostack.api.graphql;

import java.io.IOException;
import java.util.List;

/** TODO java doc **/
public interface ResponseStreamReader {
  interface NestedReader<T> {
    T read(ResponseStreamReader reader) throws IOException;
  }

  boolean hasNext() throws IOException;

  void skipNext() throws IOException;

  boolean isNextObject() throws IOException;

  boolean isNextList() throws IOException;

  boolean isNextNull() throws IOException;

  boolean isNextBoolean() throws IOException;

  boolean isNextNumber() throws IOException;

  String nextName() throws IOException;

  String nextString() throws IOException;

  String nextOptionalString() throws IOException;

  int nextInt() throws IOException;

  Integer nextOptionalInt() throws IOException;

  long nextLong() throws IOException;

  Long nextOptionalLong() throws IOException;

  double nextDouble() throws IOException;

  Double nextOptionalDouble() throws IOException;

  boolean nextBoolean() throws IOException;

  Boolean nextOptionalBoolean() throws IOException;

  <T> T nextObject(NestedReader<T> nestedReader) throws IOException;

  <T> T nextOptionalObject(NestedReader<T> nestedReader) throws IOException;

  <T> List<T> nextList(NestedReader<T> nestedReader) throws IOException;

  <T> List<T> nexOptionalList(NestedReader<T> nestedReader) throws IOException;

  ResponseReader toBufferedReader() throws IOException;
}
