package com.apollographql.apollo.api.internal;

import java.io.IOException;

public interface InputFieldMarshaller {
  void marshal(InputFieldWriter writer) throws IOException;
}
