package com.apollographql.apollo.api.internal;

public interface ResponseFieldMarshaller {
  void marshal(ResponseWriter writer);
}
