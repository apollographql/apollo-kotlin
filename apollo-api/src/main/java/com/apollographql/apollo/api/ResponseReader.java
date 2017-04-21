package com.apollographql.apollo.api;

import java.io.IOException;

/*
 * ResponseReader is responsible for converting a field object to another object of type T.
 */
public interface ResponseReader {
  <T> T read(Field field) throws IOException;
}
