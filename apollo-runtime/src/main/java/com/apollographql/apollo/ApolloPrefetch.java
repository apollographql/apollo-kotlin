package com.apollographql.apollo;

import com.apollographql.apollo.internal.util.Cancelable;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApolloPrefetch extends Cancelable {

  void execute() throws IOException;

  @Nonnull ApolloPrefetch enqueue(@Nullable Callback callback);

  ApolloPrefetch clone();

  interface Callback {
    void onSuccess();

    void onFailure(@Nonnull Throwable t);
  }
}
