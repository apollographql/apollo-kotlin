package com.apollographql.android.cache;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

final class CacheResponseBody extends ResponseBody {
  private final ResponseCacheRecord cacheRecord;
  private final String contentType;
  private final String contentLength;

  CacheResponseBody(final ResponseCacheRecord cacheRecord, Response response) {
    this.cacheRecord = cacheRecord;
    this.contentType = response.header("Content-Type");
    this.contentLength = response.header("Content-Length");
  }

  @Override public MediaType contentType() {
    return contentType != null ? MediaType.parse(contentType) : null;
  }

  @Override public long contentLength() {
    try {
      return contentLength != null ? Long.parseLong(contentLength) : -1;
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  @Override public BufferedSource source() {
    Source source = cacheRecord.bodySource();
    return Okio.buffer(new ForwardingSource(source) {
      @Override public void close() throws IOException {
        cacheRecord.close();
        super.close();
      }
    });
  }
}
