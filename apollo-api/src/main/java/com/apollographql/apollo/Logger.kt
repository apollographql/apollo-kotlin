package com.apollographql.apollo;

import com.apollographql.apollo.api.internal.Optional;

import org.jetbrains.annotations.NotNull;

/**
 * Logger to use for logging by the {@link ApolloClient}
 */
public interface Logger {
  int DEBUG = 3;
  int WARN = 5;
  int ERROR = 6;

  /**
   * Logs the message to the appropriate channel (file, console, etc)
   *
   * @param priority the priority to set
   * @param message message to log
   * @param t Optional throwable to log
   * @param args extra arguments to pass to the logged message.
   */
  void log(int priority, @NotNull String message, @NotNull Optional<Throwable> t, @NotNull Object... args);
}
