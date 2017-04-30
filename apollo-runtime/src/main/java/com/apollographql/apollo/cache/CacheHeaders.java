package com.apollographql.apollo.cache;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A key/value collection which is sent with {@link com.apollographql.apollo.cache.normalized.Record}
 * from a {@link com.apollographql.apollo.api.Operation} to the
 * {@link com.apollographql.apollo.cache.normalized.NormalizedCache}.
 *
 * For headers which the default {@link com.apollographql.apollo.cache.normalized.NormalizedCache} respect, see
 * {@link ApolloCacheHeaders}.
 */
public final class CacheHeaders {

  private Map<String, String> headerMap = new LinkedHashMap<>();

  public static CacheHeaders.Builder builder() {
    return new Builder();
  }

  public static final CacheHeaders NONE = CacheHeaders.builder().build();

  public static final class Builder {

    private Map<String, String> headerMap = new LinkedHashMap<>();

    public Builder addHeader(String headerName, String headerValue) {
      headerMap.put(headerName, headerValue);
      return this;
    }

    public Builder addHeaders(Map<String, String> headerMap) {
      this.headerMap.putAll(headerMap);
      return this;
    }

    public CacheHeaders build() {
      return new CacheHeaders(headerMap);
    }
  }

  /**
   * @return A {@link CacheHeaders.Builder} with a copy of this {@link CacheHeaders} values.
   */
  public CacheHeaders.Builder toBuilder() {
    CacheHeaders.Builder builder = builder();
    builder.addHeaders(headerMap);
    return builder;
  }

  private CacheHeaders(Map<String, String> headerMap) {
    this.headerMap = headerMap;
  }

  @Nullable
  public String headerValue(String header) {
    return headerMap.get(header);
  }

  public boolean hasHeader(String headerName) {
    return headerMap.containsKey(headerName);
  }
}
