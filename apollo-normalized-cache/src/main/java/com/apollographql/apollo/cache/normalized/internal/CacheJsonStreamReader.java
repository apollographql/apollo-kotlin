package com.apollographql.apollo.cache.normalized.internal;

import com.apollographql.apollo.api.internal.json.JsonReader;
import com.apollographql.apollo.api.internal.json.ResponseJsonStreamReader;
import com.apollographql.apollo.cache.normalized.CacheReference;

import java.io.IOException;

/**
 * A {@link ResponseJsonStreamReader} with additional support for {@link CacheReference}.
 */
public final class CacheJsonStreamReader extends ResponseJsonStreamReader {

  public CacheJsonStreamReader(JsonReader jsonReader) {
    super(jsonReader);
  }

  @Override public Object nextScalar(boolean optional) throws IOException {
    Object scalar = super.nextScalar(optional);
    if (scalar instanceof String) {
      String scalarString = (String) scalar;
      if (CacheReference.canDeserialize(scalarString)) {
        return CacheReference.deserialize(scalarString);
      }
    }
    return scalar;
  }

}
