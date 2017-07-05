package com.apollographql.apollo.api;

public interface ResponseFieldMarshaller {
  void marshal(ResponseWriter writer);
}
