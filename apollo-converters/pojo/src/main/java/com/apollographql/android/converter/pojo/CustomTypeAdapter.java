package com.apollographql.android.converter.pojo;

public interface CustomTypeAdapter<T> {

  T decode(String value);

  String encode(T value);
}
