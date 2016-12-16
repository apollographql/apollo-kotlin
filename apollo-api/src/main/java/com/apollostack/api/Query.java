package com.apollostack.api;

import java.util.Collections;
import java.util.Map;

/** TODO */
public interface Query<V extends Query.Variables> {
  /** TODO */
  String queryDocument();

  /** TODO */
  V variables();

  /** TODO */
  interface Data {
  }

  /** TODO */
  abstract class Variables {
    protected final Map<String, Object> data;

    protected Variables(Map<String, Object> data) {
      this.data = Collections.unmodifiableMap(data);
    }

    /** TODO */
    public Map<String, Object> toMap() {
      return data;
    }
  }

  Variables EMPTY_VARIABLES = new Variables(Collections.<String, Object>emptyMap()) {
  };
}
