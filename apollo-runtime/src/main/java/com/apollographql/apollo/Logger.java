package com.apollographql.apollo;

import com.apollographql.apollo.api.internal.Optional;

import javax.annotation.Nonnull;

public interface Logger {
  int DEBUG = 3;
  int WARN = 5;
  int ERROR = 6;

  void log(int priority, @Nonnull String message, @Nonnull Optional<Throwable> t, @Nonnull Object... args);
}
