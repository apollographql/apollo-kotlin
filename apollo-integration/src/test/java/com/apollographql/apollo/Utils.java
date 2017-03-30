package com.apollographql.apollo;

import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class Utils {

  private Utils() {
  }

  public static String readFileToString(final Class contextClass,
      final String streamIdentifier) throws IOException {

    InputStreamReader inputStreamReader = null;
    try {
      inputStreamReader = new InputStreamReader(contextClass.getResourceAsStream(streamIdentifier));
      return CharStreams.toString(inputStreamReader);
    } catch (IOException e) {
      throw new IOException();
    } finally {
      if (inputStreamReader != null) {
        inputStreamReader.close();
      }
    }
  }

  public static ExecutorService immediateExecutorService() {
    return new AbstractExecutorService() {
      @Override public void shutdown() {

      }

      @Override public List<Runnable> shutdownNow() {
        return null;
      }

      @Override public boolean isShutdown() {
        return false;
      }

      @Override public boolean isTerminated() {
        return false;
      }

      @Override public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
        return false;
      }

      @Override public void execute(Runnable runnable) {
        runnable.run();
      }
    };
  }
}
