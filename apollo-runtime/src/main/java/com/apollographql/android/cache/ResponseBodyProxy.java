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
import static okhttp3.internal.Util.discard;

final class ResponseBodyProxy extends ResponseBody {
  private final ResponseCacheRecordEditor cacheRecord;
  private final String contentType;
  private final String contentLength;
  private final Source responseBodySource;

  ResponseBodyProxy(ResponseCacheRecordEditor cacheRecord, Response sourceResponse) {
    this.cacheRecord = cacheRecord;
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
    final BufferedSink responseBodySink = Okio.buffer(cacheRecord.bodySink());
    return new Source() {
      boolean closed;

      @Override public long read(Buffer sink, long byteCount) throws IOException {
        long bytesRead;
        try {
          bytesRead = responseBodySource.read(sink, byteCount);
        } catch (IOException e) {
          if (!closed) {
            // Failed to write a complete cache response.
            closed = true;
            responseBodySink.close();
            cacheRecord.abort();
          }
          throw e;
        }

        if (bytesRead == -1) {
          if (!closed) {
            // The cache response is complete!
            closed = true;
            responseBodySink.close();
            cacheRecord.commit();
          }
          return -1;
        }

        sink.copyTo(responseBodySink.buffer(), sink.size() - bytesRead, bytesRead);
        responseBodySink.emitCompleteSegments();
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
          responseBodySink.close();
          cacheRecord.commit();
        } else {
          responseBodySource.close();
          responseBodySink.close();
          cacheRecord.abort();
        }
      }
    };
  }
}
