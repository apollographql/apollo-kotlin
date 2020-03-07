package com.apollographql.apollo.api.cache.http;

import okio.Sink;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface HttpCacheRecordEditor {
  @NotNull Sink headerSink();

  @NotNull Sink bodySink();

  void abort() throws IOException;

  void commit() throws IOException;
}
