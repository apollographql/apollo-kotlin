package com.example.pojo_custom_scalar_type;

import com.apollographql.android.api.graphql.ResponseFieldMapper;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class ResponseFieldMappers {
  public static final Map<Type, ResponseFieldMapper> MAPPERS = Collections.unmodifiableMap(
    new HashMap<Type, ResponseFieldMapper>() {
      {
        put(TestQuery.Data.class, new TestQuery.Data.Mapper(TestQuery.Data.FACTORY));
      }
    });

  private ResponseFieldMappers() {
  }
}
