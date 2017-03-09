package com.apollographql.android.cache.http;

public enum HttpCacheControl {
  CACHE_FIRST("cache-first"),
  CACHE_ONLY("cache-only"),
  NETWORK_ONLY("network-only"),
  NETWORK_FIRST("network-first"),
  NETWORK_BEFORE_STALE("network-before-stale"),
  EXPIRE_AFTER_READ("expire-after-read");

  public final String httpHeader;

  HttpCacheControl(String httpHeader) {
    this.httpHeader = httpHeader;
  }

  static HttpCacheControl valueOfHttpHeader(String header) {
    for (HttpCacheControl value : values()) {
      if (value.httpHeader.equals(header)) {
        return value;
      }
    }
    return null;
  }
}
