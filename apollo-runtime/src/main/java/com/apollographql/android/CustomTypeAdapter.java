package com.apollographql.android;

public interface CustomTypeAdapter<T> {

  T decode(String value);

  String encode(T value);
}
