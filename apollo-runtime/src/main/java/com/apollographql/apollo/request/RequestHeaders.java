package com.apollographql.apollo.request;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

/**
 * A key/value collection of HTTP headers which are added to a request.
 */
public final class RequestHeaders {
  private final Map<String, String> headerMap;

  public static RequestHeaders.Builder builder() {
    return new Builder();
  }

  public static final RequestHeaders NONE = new RequestHeaders(Collections.<String, String>emptyMap());

  public static final class Builder {

    private final Map<String, String> headerMap = new LinkedHashMap<>();

    public Builder addHeader(String headerName, String headerValue) {
      headerMap.put(headerName, headerValue);
      return this;
    }

    public Builder addHeaders(Map<String, String> headerMap) {
      this.headerMap.putAll(headerMap);
      return this;
    }

    public RequestHeaders build() {
      return new RequestHeaders(headerMap);
    }
  }

  /**
   * @return A {@link RequestHeaders.Builder} with a copy of this {@link RequestHeaders} values.
   */
  public RequestHeaders.Builder toBuilder() {
    RequestHeaders.Builder builder = builder();
    builder.addHeaders(headerMap);
    return builder;
  }

  RequestHeaders(Map<String, String> headerMap) {
    this.headerMap = headerMap;
  }

  public Set<String> headers() {
    return headerMap.keySet();
  }

  @Nullable
  public String headerValue(String header) {
    return headerMap.get(header);
  }

  public boolean hasHeader(String headerName) {
    return headerMap.containsKey(headerName);
  }
}
