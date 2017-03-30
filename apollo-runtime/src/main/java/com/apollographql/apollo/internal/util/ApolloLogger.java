package com.apollographql.apollo.internal.util;

import com.apollographql.apollo.Logger;
import com.apollographql.android.api.graphql.internal.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.android.api.graphql.util.Utils.checkNotNull;

public final class ApolloLogger {

  private final Optional<Logger> logger;

  public ApolloLogger(@Nonnull Optional<Logger> logger) {
    this.logger = checkNotNull(logger, "logger == null");
  }

  public void d(@Nonnull String message, Object... args) {
    log(Logger.DEBUG, message, null, args);
  }

  public void d(@Nullable Throwable t, @Nonnull String message, Object... args) {
    log(Logger.DEBUG, message, t, args);
  }

  public void w(@Nonnull String message, Object... args) {
    log(Logger.WARN, message, null, args);
  }

  public void w(@Nullable Throwable t, @Nonnull String message, Object... args) {
    log(Logger.WARN, message, t, args);
  }

  public void e(@Nonnull String message, Object... args) {
    log(Logger.ERROR, message, null, args);
  }

  public void e(@Nullable Throwable t, @Nonnull String message, Object... args) {
    log(Logger.ERROR, message, t, args);
  }

  private void log(int priority, @Nonnull String message, @Nullable Throwable t, Object... args) {
    if (logger.isPresent()) {
      logger.get().log(priority, message, Optional.fromNullable(t), args);
    }
  }
}
