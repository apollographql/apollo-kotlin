package com.apollographql.android.impl.util;

import okhttp3.Response;

public class HttpException extends RuntimeException {
  private final okhttp3.Response rawResponse;

  public HttpException(okhttp3.Response rawResponse) {
    this.rawResponse = rawResponse;
  }

  public Response rawResponse() {
    return rawResponse;
  }
}
