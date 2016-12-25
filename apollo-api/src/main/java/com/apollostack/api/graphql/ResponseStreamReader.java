package com.apollostack.api.graphql;

import java.io.IOException;
import java.util.List;

/** TODO java doc **/
public abstract class ResponseStreamReader {
  public static interface NestedReader<T> {
    T read(ResponseStreamReader reader) throws IOException;
  }

  public abstract boolean hasNext() throws IOException;

  public abstract void skipNext() throws IOException;

  public abstract boolean isNextObject() throws IOException;

  public abstract boolean isNextList() throws IOException;

  public abstract boolean isNextNull() throws IOException;

  public abstract boolean isNextBoolean() throws IOException;

  public abstract boolean isNextNumber() throws IOException;

  public abstract String nextName() throws IOException;

  public abstract String nextString() throws IOException;

  public abstract String nextOptionalString() throws IOException;

  public abstract int nextInt() throws IOException;

  public abstract Integer nextOptionalInt() throws IOException;

  public abstract long nextLong() throws IOException;

  public abstract Long nextOptionalLong() throws IOException;

  public abstract double nextDouble() throws IOException;

  public abstract Double nextOptionalDouble() throws IOException;

  public abstract boolean nextBoolean() throws IOException;

  public abstract Boolean nextOptionalBoolean() throws IOException;

  public abstract <T> T nextObject(NestedReader<T> nestedReader) throws IOException;

  public abstract <T> T nextOptionalObject(NestedReader<T> nestedReader) throws IOException;

  public abstract <T> List<T> nextList(NestedReader<T> nestedReader) throws IOException;

  public abstract <T> List<T> nexOptionalList(NestedReader<T> nestedReader) throws IOException;
}
