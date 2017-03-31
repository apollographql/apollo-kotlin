package com.apollographql.apollo.exception;

import javax.annotation.Nullable;

import okhttp3.Response;

public final class ApolloHttpException extends ApolloException {
  private final int code;
  private final String message;
  private final transient Response rawResponse;

  public ApolloHttpException(@Nullable okhttp3.Response rawResponse) {
    super(formatMessage(rawResponse));
    this.code = rawResponse != null ? rawResponse.code() : 0;
    this.message = rawResponse != null ? rawResponse.message() : "";
    this.rawResponse = rawResponse;
  }

  public ApolloHttpException(String message, Throwable cause) {
    super(message, cause);
    this.code = 0;
    this.message = message != null ? message : "";
    rawResponse = null;
  }

  public int code() {
    return code;
  }

  public String message() {
    return message;
  }

  @Nullable public Response rawResponse() {
    return rawResponse;
  }

  private static String formatMessage(Response response) {
    if (response == null) {
      return "Empty HTTP response";
    }
    return "HTTP " + response.code() + " " + response.message();
  }
}
