package com.apollographql.android.cache.normalized;

import com.apollographql.android.impl.JsonReader;
import com.apollographql.android.impl.ResponseJsonStreamReader;

import java.io.IOException;

/**
 * A {@link ResponseJsonStreamReader} with additional support for {@link CacheReference}.
 */
final class CacheJsonStreamReader extends ResponseJsonStreamReader {

  CacheJsonStreamReader(JsonReader jsonReader) {
    super(jsonReader);
  }

  @Override protected Object readScalar(ResponseJsonStreamReader streamReader) throws IOException {
    Object scalar = super.readScalar(streamReader);
    if (scalar instanceof String) {
      String scalarString = (String) scalar;
      if (CacheReference.isSerializedReference((String) scalar)) {
        return CacheReference.deserialize(scalarString);
      }
    }
    return scalar;
  }

}
