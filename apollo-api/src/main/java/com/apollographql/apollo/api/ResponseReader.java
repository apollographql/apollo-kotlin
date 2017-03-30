package com.apollographql.apollo.api;

import java.io.IOException;

/** TODO **/
public interface ResponseReader {
  <T> T read(Field field) throws IOException;
}
