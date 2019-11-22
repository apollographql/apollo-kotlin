package com.apollographql.apollo.internal.json;

import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader;
import com.apollographql.apollo.api.internal.json.ResponseJsonStreamReader;
import okio.BufferedSource;

public final class ApolloJsonReader {

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
