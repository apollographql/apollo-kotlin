package com.apollographql.apollo.internal.util;

import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class HttpResponseUtils {

  public static @NotNull Response strip(@NotNull Response response) {
    checkNotNull(response, "response == null");
    final Response.Builder builder = response.newBuilder();

    if (response.body() != null) {
      builder.body(null);
    }

    final Response cacheResponse = response.cacheResponse();
    if (cacheResponse != null) {
      builder.cacheResponse(strip(cacheResponse));
    }

    final Response networkResponse = response.networkResponse();
    if (networkResponse != null) {
      builder.networkResponse(strip(networkResponse));
    }

    return builder.build();
  }

  private HttpResponseUtils() {
  }
}
