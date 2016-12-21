package com.apollostack.android;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import static com.squareup.moshi.Types.getRawType;

public class PostBodyJsonAdapterFactory implements JsonAdapter.Factory {
  @Override public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {
    if (PostBody.class.isAssignableFrom(getRawType(type))) {
      return new PostBodyJsonAdapter(moshi);
    } else {
      return null;
    }
  }
}
