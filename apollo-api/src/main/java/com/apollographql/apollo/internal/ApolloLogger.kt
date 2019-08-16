package com.apollographql.apollo.internal;

import com.apollographql.apollo.Logger;
import com.apollographql.apollo.api.internal.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class ApolloLogger {

  private final Optional<Logger> logger;

  public ApolloLogger(@NotNull Optional<Logger> logger) {
    this.logger = checkNotNull(logger, "logger == null");
  }

  public void d(@NotNull String message, Object... args) {
    log(Logger.DEBUG, message, null, args);
  }

  public void d(@Nullable Throwable t, @NotNull String message, Object... args) {
    log(Logger.DEBUG, message, t, args);
  }

  public void w(@NotNull String message, Object... args) {
    log(Logger.WARN, message, null, args);
  }

  public void w(@Nullable Throwable t, @NotNull String message, Object... args) {
    log(Logger.WARN, message, t, args);
  }

  public void e(@NotNull String message, Object... args) {
    log(Logger.ERROR, message, null, args);
  }

  public void e(@Nullable Throwable t, @NotNull String message, Object... args) {
    log(Logger.ERROR, message, t, args);
  }

  private void log(int priority, @NotNull String message, @Nullable Throwable t, Object... args) {
    if (logger.isPresent()) {
      logger.get().log(priority, message, Optional.fromNullable(t), args);
    }
  }
}
