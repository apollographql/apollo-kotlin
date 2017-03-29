package com.apollographql.apollo;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApolloPrefetch {

  void execute() throws IOException;

  @Nonnull ApolloPrefetch enqueue(@Nullable Callback callback);

  ApolloPrefetch clone();

  void cancel();

  interface Callback {
    void onSuccess();

    void onFailure(@Nonnull Throwable t);
  }
}
