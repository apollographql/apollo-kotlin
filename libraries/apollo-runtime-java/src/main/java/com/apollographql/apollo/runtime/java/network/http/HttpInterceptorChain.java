package com.apollographql.apollo.runtime.java.network.http;

import com.apollographql.apollo.api.http.HttpRequest;
import org.jetbrains.annotations.NotNull;

public interface HttpInterceptorChain {
  void proceed(@NotNull HttpRequest request, @NotNull HttpCallback callback);
}
