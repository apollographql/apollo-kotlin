package com.apollographql.apollo.api;

import java.io.IOException;

public interface ResponseFieldMarshaller {
  void marshal(ResponseWriter writer) throws IOException;
}
