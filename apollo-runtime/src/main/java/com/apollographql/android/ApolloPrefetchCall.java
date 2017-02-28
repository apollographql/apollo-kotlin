package com.apollographql.android;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApolloPrefetchCall {

  void execute() throws IOException;

  @Nonnull ApolloPrefetchCall enqueue(@Nullable Callback callback);

  ApolloPrefetchCall clone();

  void cancel();

  interface Callback {
    void onSuccess();

    void onFailure(@Nonnull Exception e);
  }

}
