package com.apollographql.apollo.api;

import java.io.IOException;

public interface InputFieldMarshaller {
  void marshal(InputFieldWriter writer) throws IOException;
}
