package com.example.fragment_in_fragment;

import com.apollographql.android.api.graphql.ResponseFieldMapper;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ResponseFieldMappers {
  public static final Map<Type, ResponseFieldMapper> MAPPERS = Collections.unmodifiableMap(
    new HashMap<Type, ResponseFieldMapper>() {
      {
        put(TestQuery.Data.class, new TestQuery.Data.Mapper(TestQuery.Data.FACTORY));
      }
    });

  private ResponseFieldMappers() {
  }
}
