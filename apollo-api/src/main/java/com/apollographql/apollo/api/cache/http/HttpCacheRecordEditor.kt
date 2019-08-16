package com.apollographql.apollo.api.cache.http;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import okio.Sink;

public interface HttpCacheRecordEditor {
  @NotNull Sink headerSink();

  @NotNull Sink bodySink();

  void abort() throws IOException;

  void commit() throws IOException;
}
