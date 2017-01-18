package com.apollostack.api.graphql;

import java.io.IOException;

public abstract class ResponseFieldMapper<T> {

  protected abstract void handleValue(int fieldIndex, Object value, T instance, ResponseReader reader) throws IOException;

  protected abstract Field[] fields();

  public void map(final ResponseReader responseReader, final T instance) throws IOException {
    responseReader.read(new ResponseReader.ValueHandler() {
      @Override public void handle(int fieldIndex, Object value) throws IOException {
        handleValue(fieldIndex, value, instance, responseReader);
      }
    }, fields());
  }
}
