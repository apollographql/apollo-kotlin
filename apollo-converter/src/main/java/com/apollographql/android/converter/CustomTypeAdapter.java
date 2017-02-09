package com.apollographql.android.converter;

public interface CustomTypeAdapter<T> {

  T decode(String value);

  String encode(T value);
}
