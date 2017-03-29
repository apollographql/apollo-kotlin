package com.apollographql.apollo.cache.http;

import java.io.IOException;

import javax.annotation.Nonnull;

import okio.Sink;

public interface ResponseCacheRecordEditor {
  @Nonnull Sink headerSink();

  @Nonnull Sink bodySink();

  void abort() throws IOException;

  void commit() throws IOException;
}
