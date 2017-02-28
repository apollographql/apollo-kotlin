package com.apollographql.android.cache;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpCodec;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import okio.Timeout;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static okhttp3.internal.Util.closeQuietly;
import static okhttp3.internal.Util.discard;

final class ResponseBodyProxy extends ResponseBody {
  private final ResponseCacheRecordEditor cacheRecordEditor;
  private final String contentType;
  private final String contentLength;
  private final Source responseBodySource;

  ResponseBodyProxy(ResponseCacheRecordEditor cacheRecordEditor, Response sourceResponse) {
    this.cacheRecordEditor = cacheRecordEditor;
    this.contentType = sourceResponse.header("Content-Type");
    this.contentLength = sourceResponse.header("Content-Length");
    this.responseBodySource = sourceResponse.body().source();
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
    return Okio.buffer(proxySource());
  }

  private Source proxySource() {
    return new ProxySource(cacheRecordEditor, responseBodySource);
  }

  private static class ProxySource implements Source {
    final ResponseCacheRecordEditor cacheRecordEditor;
    final Source responseBodySource;
    final BufferedSink cacheResponseBodySink;
    boolean closed;
    boolean cacheFail;

    ProxySource(ResponseCacheRecordEditor cacheRecordEditor, Source responseBodySource) {
      this.cacheRecordEditor = cacheRecordEditor;
      this.responseBodySource = responseBodySource;
      cacheResponseBodySink = Okio.buffer(cacheRecordEditor.bodySink());
    }

    @Override public long read(Buffer sink, long byteCount) throws IOException {
      long bytesRead;
      try {
        bytesRead = responseBodySource.read(sink, byteCount);
      } catch (IOException e) {
        if (!closed) {
          // Failed to write a complete cache response.
          closed = true;
          abortCacheQuietly();
        }
        throw e;
      }

      if (bytesRead == -1) {
        if (!closed) {
          // The cache response is complete!
          closed = true;
          commitCache();
        }
        return -1;
      }

      try {
        if (!cacheFail) {
          sink.copyTo(cacheResponseBodySink.buffer(), sink.size() - bytesRead, bytesRead);
          cacheResponseBodySink.emitCompleteSegments();
        }
      } catch (Exception e) {
        //TODO log me
        cacheFail = true;
        abortCacheQuietly();
      }

      return bytesRead;
    }

    @Override public Timeout timeout() {
      return responseBodySource.timeout();
    }

    @Override public void close() throws IOException {
      if (closed) {
        return;
      }
      closed = true;

      if (discard(this, HttpCodec.DISCARD_STREAM_TIMEOUT_MILLIS, MILLISECONDS)) {
        responseBodySource.close();
        commitCache();
      } else {
        responseBodySource.close();
        abortCacheQuietly();
      }
    }

    private void commitCache() {
      try {
        cacheResponseBodySink.close();
        cacheRecordEditor.commit();
      } catch (Exception e) {
        //TODO log me
        closeQuietly(cacheResponseBodySink);
        abortCacheQuietly();
      }
    }

    private void abortCacheQuietly() {
      closeQuietly(cacheResponseBodySink);
      try {
        cacheRecordEditor.abort();
      } catch (Exception ignore) {
      }
    }
  }
}
