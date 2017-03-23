package com.apollographql.android.impl;

import okio.BufferedSource;

public final class ApolloReader {

  public static CacheJsonStreamReader cacheJsonStreamReader(BufferedSourceJsonReader sourceJsonReader) {
    return new CacheJsonStreamReader(sourceJsonReader);
  }

  public static ResponseJsonStreamReader responseJsonStreamReader(BufferedSourceJsonReader sourceJsonReader) {
    return new ResponseJsonStreamReader(sourceJsonReader);
  }

  public static BufferedSourceJsonReader bufferedSourceJsonReader(BufferedSource bufferedSource) {
    return new BufferedSourceJsonReader(bufferedSource);
  }
}
