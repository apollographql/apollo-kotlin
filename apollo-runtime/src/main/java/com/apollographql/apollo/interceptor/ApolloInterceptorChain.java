package com.apollographql.apollo.interceptor;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

public interface ApolloInterceptorChain {
  @Nonnull ApolloInterceptor.InterceptorResponse proceed() throws IOException;

  void proceedAsync(@Nonnull ExecutorService dispatcher, @Nonnull ApolloInterceptor.CallBack callBack);

  void dispose();
}
