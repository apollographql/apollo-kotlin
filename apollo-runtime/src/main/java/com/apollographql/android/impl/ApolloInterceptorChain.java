package com.apollographql.android.impl;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

interface ApolloInterceptorChain {
  @Nonnull ApolloInterceptor.InterceptorResponse proceed() throws IOException;

  void proceedAsync(@Nonnull ExecutorService dispatcher, @Nonnull ApolloInterceptor.CallBack callBack);

  void dispose();
}
