package com.apollographql.apollo.api;

import java.io.IOException;

/*
 * ResponseReader is an abstraction for reading GraphQL fields.
 */
public interface ResponseReader {
  <T> T read(com.apollographql.apollo.api.internal.Field field) throws IOException;
}
