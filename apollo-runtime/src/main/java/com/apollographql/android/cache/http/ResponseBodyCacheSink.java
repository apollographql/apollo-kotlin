package com.apollographql.android.cache.http;

import java.io.IOException;

import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;

class ResponseBodyCacheSink extends ForwardingSink {
  private boolean failed;

  ResponseBodyCacheSink(BufferedSink delegate) {
    super(delegate);
  }

  @Override public void write(Buffer source, long byteCount) throws IOException {
    if (failed) return;
    try {
      super.write(source, byteCount);
    } catch (Exception e) {
      failed = true;
      onException(e);
    }
  }

  @Override public void flush() throws IOException {
    if (failed) return;
    try {
      super.flush();
    } catch (Exception e) {
      failed = true;
      onException(e);
    }
  }

  @Override public void close() throws IOException {
    if (failed) return;
    try {
      super.close();
    } catch (Exception e) {
      failed = true;
      onException(e);
    }
  }

  void copyFrom(Buffer buffer, long offset, long bytesCount) {
    if (failed) return;
    try {
      BufferedSink outSink = (BufferedSink) delegate();
      buffer.copyTo(outSink.buffer(), offset, bytesCount);
      outSink.emitCompleteSegments();
    } catch (Exception e) {
      failed = true;
      onException(e);
    }
  }

  void onException(Exception e) {

  }
}
