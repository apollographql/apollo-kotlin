package com.example.unique_type_name;

import com.apollographql.android.api.graphql.ResponseFieldMapper;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ResponseFieldMappers {
  public static final Map<Type, ResponseFieldMapper> MAPPERS = Collections.unmodifiableMap(
    new HashMap<Type, ResponseFieldMapper>() {
      {
        put(HeroDetailQuery.Data.class, new HeroDetailQuery.Data.Mapper(HeroDetailQuery.Data.FACTORY));
      }
    });

  private ResponseFieldMappers() {
  }
}
