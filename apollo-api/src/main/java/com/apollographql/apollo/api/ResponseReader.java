package com.apollographql.apollo.api;

import java.io.IOException;

/** Converts the response back to the data **/
public interface ResponseReader {
  <T> T read(Field field) throws IOException;
}
