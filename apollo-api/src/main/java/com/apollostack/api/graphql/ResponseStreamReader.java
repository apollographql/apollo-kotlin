package com.apollostack.api.graphql;

import java.io.IOException;

/** TODO java doc **/
public interface ResponseStreamReader extends ResponseReader {
  boolean hasNext() throws IOException;

  void skipNext() throws IOException;

  String nextName() throws IOException;

  ResponseReader buffer() throws IOException;
}
