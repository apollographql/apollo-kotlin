package com.apollographql.apollo.interceptor;

import com.apollographql.apollo.exception.ApolloException;

import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

public interface ApolloInterceptorChain {
  @Nonnull ApolloInterceptor.InterceptorResponse proceed() throws ApolloException;

  void proceedAsync(@Nonnull ExecutorService dispatcher, @Nonnull ApolloInterceptor.CallBack callBack);

  void dispose();
}
